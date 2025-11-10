package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
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
                            parseEvent(eventType, data)
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

    private fun parseEvent(eventType: String, data: String): RemoteAgentEvent? {
        return try {
            when (eventType) {
                "clone_progress" -> {
                    val parsed = json.decodeFromString<CloneProgressData>(data)
                    RemoteAgentEvent.CloneProgress(parsed.stage, parsed.progress)
                }
                "clone_log" -> {
                    val parsed = json.decodeFromString<CloneLogData>(data)
                    RemoteAgentEvent.CloneLog(parsed.message, parsed.isError ?: false)
                }
                "iteration" -> {
                    val parsed = json.decodeFromString<IterationData>(data)
                    RemoteAgentEvent.Iteration(parsed.current, parsed.max)
                }
                "llm_chunk" -> {
                    val parsed = json.decodeFromString<LLMChunkData>(data)
                    RemoteAgentEvent.LLMChunk(parsed.chunk)
                }
                "tool_call" -> {
                    val parsed = json.decodeFromString<ToolCallData>(data)
                    RemoteAgentEvent.ToolCall(parsed.toolName, parsed.params)
                }
                "tool_result" -> {
                    val parsed = json.decodeFromString<ToolResultData>(data)
                    RemoteAgentEvent.ToolResult(parsed.toolName, parsed.success, parsed.output)
                }
                "error" -> {
                    val parsed = json.decodeFromString<ErrorData>(data)
                    RemoteAgentEvent.Error(parsed.message)
                }
                "complete" -> {
                    val parsed = json.decodeFromString<CompleteData>(data)
                    RemoteAgentEvent.Complete(
                        parsed.success,
                        parsed.message,
                        parsed.iterations,
                        parsed.steps,
                        parsed.edits
                    )
                }
                else -> {
                    println("Unknown SSE event type: $eventType")
                    null
                }
            }
        } catch (e: Exception) {
            println("Failed to parse SSE event: $e")
            null
        }
    }

    fun close() {
        httpClient.close()
    }
}

/**
 * Remote Agent Events - mirrors AgentEvent from mpp-server
 */
sealed class RemoteAgentEvent {
    data class CloneProgress(val stage: String, val progress: Int?) : RemoteAgentEvent()
    data class CloneLog(val message: String, val isError: Boolean) : RemoteAgentEvent()
    data class Iteration(val current: Int, val max: Int) : RemoteAgentEvent()
    data class LLMChunk(val chunk: String) : RemoteAgentEvent()
    data class ToolCall(val toolName: String, val params: String) : RemoteAgentEvent()
    data class ToolResult(val toolName: String, val success: Boolean, val output: String?) : RemoteAgentEvent()
    data class Error(val message: String) : RemoteAgentEvent()
    data class Complete(
        val success: Boolean,
        val message: String,
        val iterations: Int,
        val steps: List<AgentStepInfo>,
        val edits: List<AgentEditInfo>
    ) : RemoteAgentEvent()
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

@Serializable
data class AgentStepInfo(
    val step: Int,
    val action: String,
    val tool: String,
    val success: Boolean
)

@Serializable
data class AgentEditInfo(
    val file: String,
    val operation: String,
    val content: String
)

/**
 * Internal data classes for SSE parsing
 */
@Serializable
private data class CloneProgressData(val stage: String, val progress: Int?)

@Serializable
private data class CloneLogData(val message: String, val isError: Boolean? = false)

@Serializable
private data class IterationData(val current: Int, val max: Int)

@Serializable
private data class LLMChunkData(val chunk: String)

@Serializable
private data class ToolCallData(val toolName: String, val params: String)

@Serializable
private data class ToolResultData(val toolName: String, val success: Boolean, val output: String?)

@Serializable
private data class ErrorData(val message: String)

@Serializable
private data class CompleteData(
    val success: Boolean,
    val message: String,
    val iterations: Int,
    val steps: List<AgentStepInfo>,
    val edits: List<AgentEditInfo>
)

/**
 * Exception for remote agent errors
 */
class RemoteAgentException(message: String, cause: Throwable? = null) : Exception(message, cause)

