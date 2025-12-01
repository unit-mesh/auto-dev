package cc.unitmesh.devins.idea.toolwindow.remote

import cc.unitmesh.agent.RemoteAgentEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration.Companion.seconds

/**
 * Remote Agent Client for IntelliJ IDEA plugin.
 *
 * Connects to mpp-server and streams agent execution events via SSE.
 * This is adapted from mpp-ui's RemoteAgentClient for use in the IDE plugin.
 */
class IdeaRemoteAgentClient(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(SSE) {
            reconnectionTime = 30.seconds
            maxReconnectionAttempts = 3
        }

        // We handle HTTP errors manually to provide better error messages
        // SSE connections need explicit status checking
        expectSuccess = false

        engine {
            maxConnectionsCount = 1000
            endpoint {
                maxConnectionsPerRoute = 100
                pipelineMaxSize = 20
                keepAliveTime = 5000
                connectTimeout = 5000
                connectAttempts = 5
            }
        }
    }

    private val gson = Gson()

    /**
     * Health check to verify server is running
     */
    suspend fun healthCheck(): HealthResponse {
        val response = httpClient.get("$baseUrl/health")
        if (!response.status.isSuccess()) {
            throw RemoteAgentException("Health check failed: ${response.status}")
        }
        return gson.fromJson(response.bodyAsText(), HealthResponse::class.java)
    }

    /**
     * Get list of available projects from server
     */
    suspend fun getProjects(): ProjectListResponse {
        val response = httpClient.get("$baseUrl/api/projects")
        if (!response.status.isSuccess()) {
            throw RemoteAgentException("Failed to fetch projects: ${response.status}")
        }
        return gson.fromJson(response.bodyAsText(), ProjectListResponse::class.java)
    }

    /**
     * Execute agent task with SSE streaming
     * Returns a Flow of RemoteAgentEvent for reactive processing
     */
    fun executeStream(request: RemoteAgentRequest): Flow<RemoteAgentEvent> = flow {
        try {
            httpClient.sse(
                urlString = "$baseUrl/api/agent/stream",
                request = {
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    setBody(gson.toJson(request))
                }
            ) {
                // Check HTTP status before processing SSE events
                if (!call.response.status.isSuccess()) {
                    throw RemoteAgentException("Stream connection failed: ${call.response.status}")
                }

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
data class RemoteAgentRequest(
    val projectId: String,
    val task: String,
    val llmConfig: LLMConfig? = null,
    val gitUrl: String? = null,
    val branch: String? = null,
    val username: String? = null,
    val password: String? = null
)

data class LLMConfig(
    val provider: String,
    val modelName: String,
    val apiKey: String,
    val baseUrl: String? = null
)

data class HealthResponse(
    val status: String
)

data class ProjectInfo(
    val id: String,
    val name: String,
    val path: String,
    val description: String
)

data class ProjectListResponse(
    val projects: List<ProjectInfo>
)

/**
 * Exception for remote agent errors
 */
class RemoteAgentException(message: String, cause: Throwable? = null) : Exception(message, cause)

