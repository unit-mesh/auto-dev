package cc.unitmesh.server.plugins

import cc.unitmesh.server.auth.AuthService
import cc.unitmesh.server.session.SessionManager
import cc.unitmesh.server.service.AgentService
import cc.unitmesh.session.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * Session 路由配置
 */
fun Route.sessionRouting(
    sessionManager: SessionManager,
    authService: AuthService,
    agentService: AgentService
) {
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    
    // 认证路由
    route("/api/auth") {
        // 登录
        post("/login") {
            val request = call.receive<LoginRequest>()
            val response = authService.login(request.username, request.password)
            
            if (response.success) {
                call.respond(HttpStatusCode.OK, response)
            } else {
                call.respond(HttpStatusCode.Unauthorized, response)
            }
        }
        
        // 注册
        post("/register") {
            val request = call.receive<LoginRequest>()
            val response = authService.register(request.username, request.password)
            
            if (response.success) {
                call.respond(HttpStatusCode.Created, response)
            } else {
                call.respond(HttpStatusCode.BadRequest, response)
            }
        }
        
        // 登出
        post("/logout") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (token != null) {
                authService.logout(token)
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out successfully"))
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing authorization token"))
            }
        }
        
        // 验证 token
        get("/validate") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            if (token != null) {
                val username = authService.validateToken(token)
                if (username != null) {
                    call.respond(HttpStatusCode.OK, mapOf("valid" to true, "username" to username))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("valid" to false))
                }
            } else {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing authorization token"))
            }
        }
    }
    
    // Session 路由（需要认证）
    route("/api/sessions") {
        
        // 创建会话
        post {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@post
            }
            
            val request = call.receive<CreateSessionRequest>()
            val session = sessionManager.createSession(request.copy(userId = username))
            call.respond(HttpStatusCode.Created, session)
        }
        
        // 获取所有会话（当前用户）
        get {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@get
            }
            
            val sessions = sessionManager.getSessionsByOwner(username)
            call.respond(sessions)
        }
        
        // 获取活跃会话（当前用户）
        get("/active") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@get
            }
            
            val sessions = sessionManager.getActiveSessionsByOwner(username)
            call.respond(sessions)
        }
        
        // 获取指定会话
        get("/{sessionId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@get
            }
            
            val sessionId = call.parameters["sessionId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing sessionId")
            )
            
            val session = sessionManager.getSession(sessionId)
            if (session != null) {
                // 检查权限
                if (session.ownerId != username) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                    return@get
                }
                call.respond(session)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
            }
        }
        
        // 获取会话状态快照
        get("/{sessionId}/state") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@get
            }
            
            val sessionId = call.parameters["sessionId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing sessionId")
            )
            
            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@get
            }
            
            // 检查权限
            if (session.ownerId != username) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }
            
            val state = sessionManager.getSessionState(sessionId)
            if (state != null) {
                call.respond(state)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session state not found"))
            }
        }
        
        // 订阅会话事件（SSE）
        route("/{sessionId}/stream", HttpMethod.Get) {
            sse {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                val username = token?.let { authService.validateToken(it) }
                
                if (username == null) {
                    send(ServerSentEvent(
                        data = """{"error": "Unauthorized"}""",
                        event = "error"
                    ))
                    return@sse
                }
                
                val sessionId = call.parameters["sessionId"]
                if (sessionId == null) {
                    send(ServerSentEvent(
                        data = """{"error": "Missing sessionId"}""",
                        event = "error"
                    ))
                    return@sse
                }
                
                val session = sessionManager.getSession(sessionId)
                if (session == null) {
                    send(ServerSentEvent(
                        data = """{"error": "Session not found"}""",
                        event = "error"
                    ))
                    return@sse
                }
                
                // 检查权限
                if (session.ownerId != username) {
                    send(ServerSentEvent(
                        data = """{"error": "Access denied"}""",
                        event = "error"
                    ))
                    return@sse
                }
                
                logger.info { "User $username subscribing to session $sessionId via SSE" }
                
                try {
                    sessionManager.subscribeToSession(sessionId, username).collect { envelope ->
                        send(
                            ServerSentEvent(
                                data = json.encodeToString(envelope),
                                event = "session_event",
                                id = envelope.eventId
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error in SSE stream for session $sessionId" }
                    send(
                        ServerSentEvent(
                            data = """{"error": "${e.message}"}""",
                            event = "error"
                        )
                    )
                }
            }
        }
        
        // 启动会话执行（结合 Agent）
        post("/{sessionId}/execute") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@post
            }
            
            val sessionId = call.parameters["sessionId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing sessionId")
            )
            
            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@post
            }
            
            // 检查权限（只有 owner 可以执行）
            if (session.ownerId != username) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only session owner can execute"))
                return@post
            }
            
            // 这里可以启动 Agent 执行
            // 暂时返回成功响应
            call.respond(HttpStatusCode.Accepted, mapOf("message" to "Session execution started"))
        }
        
        // 删除会话
        delete("/{sessionId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            val username = token?.let { authService.validateToken(it) }
            
            if (username == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
                return@delete
            }
            
            val sessionId = call.parameters["sessionId"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing sessionId")
            )
            
            val session = sessionManager.getSession(sessionId)
            if (session == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                return@delete
            }
            
            // 检查权限
            if (session.ownerId != username) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@delete
            }
            
            sessionManager.deleteSession(sessionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

