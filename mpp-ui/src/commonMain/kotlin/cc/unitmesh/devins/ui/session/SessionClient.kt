package cc.unitmesh.devins.ui.session

import cc.unitmesh.session.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * SessionClient - 会话管理客户端
 * 负责与 mpp-server 的 Session API 交互
 */
class SessionClient(
    private val baseUrl: String,
    val httpClient: HttpClient = HttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    var authToken: String? = null
        private set

    /**
     * 设置认证 token
     */
    fun setAuthToken(token: String?) {
        authToken = token
    }

    /**
     * 登录
     */
    suspend fun login(username: String, password: String): LoginResponse {
        val response = httpClient.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }

        val responseText = response.bodyAsText()
        val loginResponse = json.decodeFromString<LoginResponse>(responseText)

        if (loginResponse.success && loginResponse.token != null) {
            setAuthToken(loginResponse.token)
        }

        return loginResponse
    }

    /**
     * 注册
     */
    suspend fun register(username: String, password: String): LoginResponse {
        val response = httpClient.post("$baseUrl/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }

        val responseText = response.bodyAsText()
        val loginResponse = json.decodeFromString<LoginResponse>(responseText)

        if (loginResponse.success && loginResponse.token != null) {
            setAuthToken(loginResponse.token)
        }

        return loginResponse
    }

    /**
     * 登出
     */
    suspend fun logout() {
        if (authToken != null) {
            httpClient.post("$baseUrl/api/auth/logout") {
                header("Authorization", "Bearer $authToken")
            }
            setAuthToken(null)
        }
    }

    /**
     * 验证 token
     */
    suspend fun validateToken(): Boolean {
        if (authToken == null) return false

        try {
            val response = httpClient.get("$baseUrl/api/auth/validate") {
                header("Authorization", "Bearer $authToken")
            }
            return response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 创建会话
     */
    suspend fun createSession(projectId: String, task: String, metadata: SessionMetadata? = null): Session {
        val requestBody = """
            {
                "projectId":"$projectId",
                "task":"$task",
                "userId":"",
                ${if (metadata != null) """"metadata":${json.encodeToString(SessionMetadata.serializer(), metadata)}""" else ""}
            }
        """.trimIndent()

        val response = httpClient.post("$baseUrl/api/sessions") {
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString<Session>(responseText)
    }

    /**
     * 获取所有会话
     */
    suspend fun getSessions(): List<Session> {
        val response = httpClient.get("$baseUrl/api/sessions") {
            header("Authorization", "Bearer $authToken")
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString<List<Session>>(responseText)
    }

    /**
     * 获取活跃会话
     */
    suspend fun getActiveSessions(): List<Session> {
        val response = httpClient.get("$baseUrl/api/sessions/active") {
            header("Authorization", "Bearer $authToken")
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString<List<Session>>(responseText)
    }

    /**
     * 获取指定会话
     */
    suspend fun getSession(sessionId: String): Session {
        val response = httpClient.get("$baseUrl/api/sessions/$sessionId") {
            header("Authorization", "Bearer $authToken")
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString<Session>(responseText)
    }

    /**
     * 获取会话状态快照
     */
    suspend fun getSessionState(sessionId: String): SessionState {
        val response = httpClient.get("$baseUrl/api/sessions/$sessionId/state") {
            header("Authorization", "Bearer $authToken")
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString<SessionState>(responseText)
    }

    /**
     * 订阅会话事件流（SSE）
     */
    fun subscribeToSession(sessionId: String): Flow<SessionEventEnvelope> = flow {
        httpClient.prepareGet("$baseUrl/api/sessions/$sessionId/stream") {
            header("Authorization", "Bearer $authToken")
            header("Accept", "text/event-stream")
        }.execute { response ->
            val channel = response.bodyAsChannel()

            var currentEvent = ""
            var currentData = ""

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break

                when {
                    line.startsWith("event:") -> {
                        currentEvent = line.substringAfter("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        currentData += line.substringAfter("data:").trim()
                    }
                    line.isEmpty() && currentData.isNotEmpty() -> {
                        if (currentEvent == "session_event") {
                            try {
                                val envelope = json.decodeFromString<SessionEventEnvelope>(currentData)
                                emit(envelope)
                            } catch (e: Exception) {
                                println("Error parsing session event: ${e.message}")
                            }
                        } else if (currentEvent == "error") {
                            println("SSE Error: $currentData")
                        }
                        currentEvent = ""
                        currentData = ""
                    }
                }
            }
        }
    }

    /**
     * 启动会话执行
     */
    suspend fun executeSession(
        sessionId: String,
        gitUrl: String? = null,
        branch: String? = null,
        username: String? = null,
        password: String? = null
    ) {
        val requestBody = buildMap {
            gitUrl?.let { put("gitUrl", it) }
            branch?.let { put("branch", it) }
            username?.let { put("username", it) }
            password?.let { put("password", it) }
        }

        httpClient.post("$baseUrl/api/sessions/$sessionId/execute") {
            header("Authorization", "Bearer $authToken")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(requestBody))
        }
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(sessionId: String) {
        httpClient.delete("$baseUrl/api/sessions/$sessionId") {
            header("Authorization", "Bearer $authToken")
        }
    }
}

