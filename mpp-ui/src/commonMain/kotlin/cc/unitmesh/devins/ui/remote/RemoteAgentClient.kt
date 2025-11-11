package cc.unitmesh.devins.ui.remote

import cc.unitmesh.agent.*
import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Platform-specific HttpClient factory
internal expect fun createHttpClient(): HttpClient

/**
 * Remote Agent Client for Compose
 *
 * Connects to mpp-server and streams agent execution events via SSE.
 * This is the Kotlin/Compose equivalent of ServerAgentClient.ts
 */
class RemoteAgentClient(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val httpClient: HttpClient = createHttpClient()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Health check to verify server is running
     */
    suspend fun healthCheck(): HealthResponse {
        val response = httpClient.get("$baseUrl/health")
        return json.decodeFromString(response.bodyAsText())
    }

    /**
     * Get list of available projects from server
     */
    suspend fun getProjects(): ProjectListResponse {
        val response = httpClient.get("$baseUrl/api/projects")
        return json.decodeFromString(response.bodyAsText())
    }

    /**
     * Execute agent task with SSE streaming
     * Returns a Flow of AgentEvent for reactive processing
     */
    fun executeStream(request: RemoteAgentRequest): Flow<RemoteAgentEvent> = flow {
        try {
            // Use proper SSE client with POST body
            httpClient.sse(
                urlString = "$baseUrl/api/agent/stream",
                request = {
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(RemoteAgentRequest.serializer(), request))
                }
            ) {
                // Process SSE events using incoming flow
                incoming
                    .mapNotNull { event ->
                        event.data?.takeIf { data ->
                            !data.trim().equals("[DONE]", ignoreCase = true)
                        }?.let { data ->
                            val eventType = event.event ?: "message"
                            RemoteAgentEvent.from(eventType, data)
                        }
                    }
                    .collect { parsedEvent ->
                        emit(parsedEvent)
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RemoteAgentException("Stream connection failed: ${e.message}", e)
        }
    }

    fun close() {
        httpClient.close()
    }
}


/**
 * Request/Response Data Classes
 */
@Serializable
data class RemoteAgentRequest(
    val projectId: String,
    val task: String,
    val llmConfig: LLMConfig? = null,
    val gitUrl: String? = null,
    val branch: String? = null,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class LLMConfig(
    val provider: String,
    val modelName: String,
    val apiKey: String,
    val baseUrl: String? = null
)

@Serializable
data class HealthResponse(
    val status: String
)

@Serializable
data class ProjectInfo(
    val id: String,
    val name: String,
    val path: String,
    val description: String
)

@Serializable
data class ProjectListResponse(
    val projects: List<ProjectInfo>
)

/**
 * Exception for remote agent errors
 */
class RemoteAgentException(message: String, cause: Throwable? = null) : Exception(message, cause)

