package cc.unitmesh.devti.process

import java.util.concurrent.ConcurrentHashMap

/**
 * Represents the status of a process
 */
enum class ProcessStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    KILLED
}

/**
 * Contains information about a managed process
 */
data class ProcessInfo(
    val processId: String,
    val command: String,
    val workingDirectory: String,
    val status: ProcessStatus,
    val exitCode: Int? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val environment: Map<String, String> = emptyMap(),
    val waitForCompletion: Boolean = false,
    val timeoutSeconds: Int = 0,
    val showInTerminal: Boolean = false
)

/**
 * Result of a process execution
 */
data class ProcessExecutionResult(
    val processId: String,
    val exitCode: Int?,
    val stdout: String = "",
    val stderr: String = "",
    val timedOut: Boolean = false,
    val status: ProcessStatus
)

/**
 * Request to launch a new process
 */
data class LaunchProcessRequest(
    val command: String,
    val workingDirectory: String = "",
    val environment: Map<String, String> = emptyMap(),
    val waitForCompletion: Boolean = false,
    val timeoutSeconds: Int = 30,
    val showInTerminal: Boolean = false
)

/**
 * Request to list processes
 */
data class ListProcessesRequest(
    val includeTerminated: Boolean = false,
    val maxResults: Int = 50
)

/**
 * Request to kill a process
 */
data class KillProcessRequest(
    val processId: String,
    val force: Boolean = false
)

/**
 * Response for kill process operation
 */
data class KillProcessResponse(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Request to read process output
 */
data class ReadProcessOutputRequest(
    val processId: String,
    val includeStdout: Boolean = true,
    val includeStderr: Boolean = true,
    val maxBytes: Int = 10000
)

/**
 * Response for read process output operation
 */
data class ReadProcessOutputResponse(
    val stdout: String = "",
    val stderr: String = "",
    val hasMore: Boolean = false
)

/**
 * Request to write process input
 */
data class WriteProcessInputRequest(
    val processId: String,
    val inputData: String,
    val appendNewline: Boolean = true
)

/**
 * Response for write process input operation
 */
data class WriteProcessInputResponse(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Context for tool execution
 */
data class ToolContext(
    val sessionId: String,
    val userId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Response from tool execution
 */
sealed class ToolResponse {
    data class Success(val data: Any) : ToolResponse()
    data class Error(val message: String, val cause: Throwable? = null) : ToolResponse()
    
    companion object {
        fun success(data: Any): ToolResponse = Success(data)
        fun error(message: String, cause: Throwable? = null): ToolResponse = Error(message, cause)
    }
}

/**
 * Storage for process output streams
 */
class ProcessOutputStorage {
    private val stdoutStorage = ConcurrentHashMap<String, StringBuilder>()
    private val stderrStorage = ConcurrentHashMap<String, StringBuilder>()
    
    fun appendStdout(processId: String, data: String) {
        stdoutStorage.computeIfAbsent(processId) { StringBuilder() }.append(data)
    }
    
    fun appendStderr(processId: String, data: String) {
        stderrStorage.computeIfAbsent(processId) { StringBuilder() }.append(data)
    }
    
    fun getStdout(processId: String, maxBytes: Int = Int.MAX_VALUE): String {
        val output = stdoutStorage[processId]?.toString() ?: ""
        return if (output.length > maxBytes) {
            output.substring(0, maxBytes)
        } else {
            output
        }
    }
    
    fun getStderr(processId: String, maxBytes: Int = Int.MAX_VALUE): String {
        val output = stderrStorage[processId]?.toString() ?: ""
        return if (output.length > maxBytes) {
            output.substring(0, maxBytes)
        } else {
            output
        }
    }
    
    fun hasMoreStdout(processId: String, maxBytes: Int): Boolean {
        val output = stdoutStorage[processId]?.toString() ?: ""
        return output.length > maxBytes
    }
    
    fun hasMoreStderr(processId: String, maxBytes: Int): Boolean {
        val output = stderrStorage[processId]?.toString() ?: ""
        return output.length > maxBytes
    }
    
    fun clearOutput(processId: String) {
        stdoutStorage.remove(processId)
        stderrStorage.remove(processId)
    }
}
