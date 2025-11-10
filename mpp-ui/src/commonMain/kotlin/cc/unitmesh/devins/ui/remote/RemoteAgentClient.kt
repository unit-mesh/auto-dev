package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        val response = httpClient.post("$baseUrl/api/agent/stream") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "text/event-stream")
            setBody(json.encodeToString(RemoteAgentRequest.serializer(), request))
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE // No timeout for streaming
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = Long.MAX_VALUE
            }
        }

        if (!response.status.isSuccess()) {
            throw RemoteAgentException("Server error: ${response.status.value} - ${response.bodyAsText()}")
        }

        // Parse SSE stream in real-time
        val channel = response.bodyAsChannel()
        parseSSEStreamRealtime(channel).collect { event ->
            emit(event)
        }
    }

    /**
     * Parse SSE stream format in real-time:
     * event: event_type
     * data: {"key": "value"}
     *
     * (blank line separates events)
     */
    private fun parseSSEStreamRealtime(channel: ByteReadChannel): Flow<RemoteAgentEvent> = flow {
        var currentEventType: String? = null
        var currentData: String? = null

        try {
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                when {
                    line.startsWith("event:") -> {
                        currentEventType = line.removePrefix("event:").trim()
                    }
                    line.startsWith("data:") -> {
                        currentData = line.removePrefix("data:").trim()
                    }
                    line.isBlank() && currentEventType != null && currentData != null -> {
                        // Complete event - parse and emit immediately
                        val event = parseEvent(currentEventType, currentData)
                        if (event != null) {
                            emit(event)

                            // Stop on complete event
                            if (event is RemoteAgentEvent.Complete) {
                                return@flow
                            }
                        }

                        // Reset for next event
                        currentEventType = null
                        currentData = null
                    }
                }
            }
        } catch (e: Exception) {
            println("Error reading SSE stream: ${e.message}")
            throw RemoteAgentException("Stream reading failed: ${e.message}", e)
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

