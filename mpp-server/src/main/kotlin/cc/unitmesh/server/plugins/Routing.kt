package cc.unitmesh.server.plugins

import cc.unitmesh.agent.AgentEvent
import cc.unitmesh.server.auth.AuthService
import cc.unitmesh.server.config.ServerConfig
import cc.unitmesh.server.model.*
import cc.unitmesh.server.service.AgentService
import cc.unitmesh.server.service.ProjectService
import cc.unitmesh.server.session.SessionManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun Application.configureRouting() {
    val config = ServerConfig.load()
    val projectService = ProjectService(config.projects)
    val agentService = AgentService(config.llm)
    
    // 初始化会话管理和认证服务
    val sessionManager = SessionManager()
    val authService = AuthService()

    // JSON serializer with polymorphic support for AgentEvent
    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(AgentEvent::class) {
                subclass(AgentEvent.IterationStart::class)
                subclass(AgentEvent.LLMResponseChunk::class)
                subclass(AgentEvent.ToolCall::class)
                subclass(AgentEvent.ToolResult::class)
                subclass(AgentEvent.CloneLog::class)
                subclass(AgentEvent.CloneProgress::class)
                subclass(AgentEvent.Error::class)
                subclass(AgentEvent.Complete::class)
            }
        }
    }

    routing {
        // Health check
        get("/health") {
            call.respond(HealthResponse(status = "ok"))
        }

        // API routes
        route("/api") {
            // Project management
            route("/projects") {
                get {
                    val projects = projectService.listProjects()
                    call.respond(ProjectListResponse(projects = projects))
                }

                get("/{id}") {
                    val projectId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing project ID")
                    )

                    val project = projectService.getProject(projectId)
                    if (project == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Project not found")
                        )
                    } else {
                        call.respond(project)
                    }
                }
            }

            // Agent execution
            route("/agent") {
                // Synchronous execution
                post("/run") {
                    val request = call.receive<AgentRequest>()

                    val project = projectService.getProject(request.projectId)
                    if (project == null) {
                        return@post call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("error" to "Project not found")
                        )
                    }

                    try {
                        val response = agentService.executeAgent(project.path, request)
                        call.respond(response)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to (e.message ?: "Unknown error"))
                        )
                    }
                }

                // SSE Streaming execution - Support POST method with SSE
                // Use route + sse to properly handle POST requests with Accept: text/event-stream
                route("/stream", HttpMethod.Post) {
                    // Handle non-SSE Accept headers (return 406)
                    handle {
                        val accept = call.request.headers[HttpHeaders.Accept]
                        if (accept != null && !accept.contains("text/event-stream")) {
                            call.respond(HttpStatusCode.NotAcceptable, mapOf("error" to "Only text/event-stream is supported"))
                        }
                    }
                    
                    sse {
                        // Parse request from POST body
                        val request = try {
                            call.receive<AgentRequest>()
                        } catch (e: Exception) {
                            send(ServerSentEvent(
                                data = json.encodeToString(AgentEvent.Error("Invalid request body: ${e.message}")),
                                event = "error"
                            ))
                            return@sse
                        }

                        // Validate required fields
                        if (request.projectId.isBlank()) {
                            send(ServerSentEvent(
                                data = json.encodeToString(AgentEvent.Error("Missing projectId")),
                                event = "error"
                            ))
                            return@sse
                        }
                        
                        if (request.task.isBlank()) {
                            send(ServerSentEvent(
                                data = json.encodeToString(AgentEvent.Error("Missing task")),
                                event = "error"
                            ))
                            return@sse
                        }

                        // If gitUrl is provided, use it; otherwise look up existing project
                        val projectPath = if (request.gitUrl.isNullOrBlank()) {
                            val project = projectService.getProject(request.projectId)
                            if (project == null) {
                                send(ServerSentEvent(
                                    data = json.encodeToString(AgentEvent.Error("Project not found: ${request.projectId}")),
                                    event = "error"
                                ))
                                return@sse
                            }
                            project.path
                        } else {
                            // Will be cloned by AgentService
                            ""
                        }

                        // Stream events
                        try {
                            agentService.executeAgentStream(projectPath, request).collect { event ->
                                val eventType = when (event) {
                                    is AgentEvent.IterationStart -> "iteration"
                                    is AgentEvent.LLMResponseChunk -> "llm_chunk"
                                    is AgentEvent.ToolCall -> "tool_call"
                                    is AgentEvent.ToolResult -> "tool_result"
                                    is AgentEvent.CloneLog -> "clone_log"
                                    is AgentEvent.CloneProgress -> "clone_progress"
                                    is AgentEvent.Error -> "error"
                                    is AgentEvent.Complete -> "complete"
                                }

                                val data = when (event) {
                                    is AgentEvent.IterationStart -> json.encodeToString(event)
                                    is AgentEvent.LLMResponseChunk -> json.encodeToString(event)
                                    is AgentEvent.ToolCall -> json.encodeToString(event)
                                    is AgentEvent.ToolResult -> json.encodeToString(event)
                                    is AgentEvent.CloneLog -> json.encodeToString(event)
                                    is AgentEvent.CloneProgress -> json.encodeToString(event)
                                    is AgentEvent.Error -> json.encodeToString(event)
                                    is AgentEvent.Complete -> json.encodeToString(event)
                                }

                                // Send SSE event
                                send(ServerSentEvent(data = data, event = eventType))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val errorData = json.encodeToString(AgentEvent.Error("Execution failed: ${e.message}"))
                            send(ServerSentEvent(data = errorData, event = "error"))
                        }
                    }
                }
                
                // Also support GET for simple cases (backward compatibility)
                sse("/stream") {
                    val projectId = call.parameters["projectId"] ?: run {
                        send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Missing projectId parameter"))))
                        return@sse
                    }
                    
                    val task = call.parameters["task"] ?: run {
                        send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Missing task parameter"))))
                        return@sse
                    }
                    
                    // Optional git clone parameters
                    val gitUrl = call.parameters["gitUrl"]
                    val branch = call.parameters["branch"]
                    val username = call.parameters["username"]
                    val password = call.parameters["password"]

                    // If gitUrl is provided, use it; otherwise look up existing project
                    val projectPath = if (gitUrl.isNullOrBlank()) {
                        val project = projectService.getProject(projectId)
                        if (project == null) {
                            send(ServerSentEvent(json.encodeToString(AgentEvent.Error("Project not found"))))
                            return@sse
                        }
                        project.path
                    } else {
                        // Will be cloned by AgentService
                        ""
                    }

                    val request = AgentRequest(
                        projectId = projectId,
                        task = task,
                        gitUrl = gitUrl,
                        branch = branch,
                        username = username,
                        password = password
                    )

                    try {
                        agentService.executeAgentStream(projectPath, request).collect { event ->
                            val eventType = when (event) {
                                is AgentEvent.IterationStart -> "iteration"
                                is AgentEvent.LLMResponseChunk -> "llm_chunk"
                                is AgentEvent.ToolCall -> "tool_call"
                                is AgentEvent.ToolResult -> "tool_result"
                                is AgentEvent.CloneLog -> "clone_log"
                                is AgentEvent.CloneProgress -> "clone_progress"
                                is AgentEvent.Error -> "error"
                                is AgentEvent.Complete -> "complete"
                            }

                            val data = when (event) {
                                is AgentEvent.IterationStart -> json.encodeToString(event)
                                is AgentEvent.LLMResponseChunk -> json.encodeToString(event)
                                is AgentEvent.ToolCall -> json.encodeToString(event)
                                is AgentEvent.ToolResult -> json.encodeToString(event)
                                is AgentEvent.CloneLog -> json.encodeToString(event)
                                is AgentEvent.CloneProgress -> json.encodeToString(event)
                                is AgentEvent.Error -> json.encodeToString(event)
                                is AgentEvent.Complete -> json.encodeToString(event)
                            }

                            // Send SSE event
                            send(ServerSentEvent(data = data, event = eventType))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val errorData = json.encodeToString(AgentEvent.Error("Execution failed: ${e.message}"))
                        send(ServerSentEvent(data = errorData, event = "error"))
                    }
                }
            }
        }
        
        // Session 和认证路由
        sessionRouting(sessionManager, authService, agentService)
    }
}

