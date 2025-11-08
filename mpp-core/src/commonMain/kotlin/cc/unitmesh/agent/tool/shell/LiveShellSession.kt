package cc.unitmesh.agent.tool.shell

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents a live shell session that can stream output in real-time.
 * This is used on platforms that support PTY (pseudo-terminal) for rich terminal emulation.
 */
data class LiveShellSession(
    val sessionId: String,
    val command: String,
    val workingDirectory: String?,
    /**
     * Platform-specific handle to the PTY process.
     * On JVM: This will be a PtyProcess instance
     * On other platforms: null (falls back to buffered output)
     */
    val ptyHandle: Any? = null,
    val isLiveSupported: Boolean = ptyHandle != null
) {
    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()
    
    private val _exitCode = MutableStateFlow<Int?>(null)
    val exitCode: StateFlow<Int?> = _exitCode.asStateFlow()
    
    private val _stdout = StringBuilder()
    private val _stderr = StringBuilder()
    
    /**
     * Get the captured stdout output
     */
    fun getStdout(): String = _stdout.toString()
    
    /**
     * Get the captured stderr output
     */
    fun getStderr(): String = _stderr.toString()
    
    /**
     * Append output to stdout (called by executor)
     */
    internal fun appendStdout(text: String) {
        _stdout.append(text)
    }
    
    /**
     * Append output to stderr (called by executor)
     */
    internal fun appendStderr(text: String) {
        _stderr.append(text)
    }
    
    fun markCompleted(exitCode: Int) {
        _exitCode.value = exitCode
        _isCompleted.value = true
    }
    
    /**
     * Wait for the session to complete (expected to be overridden or handled platform-specifically)
     * Returns the exit code, or throws if timeout/error occurs
     */
    suspend fun waitForCompletion(timeoutMs: Long): Int {
        throw UnsupportedOperationException("waitForCompletion must be handled platform-specifically")
    }
}

/**
 * Interface for shell executors that support live streaming
 */
interface LiveShellExecutor {
    /**
     * Check if live shell execution is supported on this platform
     */
    fun supportsLiveExecution(): Boolean = false
    
    /**
     * Start a shell command with live output streaming.
     * Returns a LiveShellSession that the UI can connect to.
     * 
     * Note: The command will start executing immediately, and the UI should
     * connect to the PTY handle as soon as possible to avoid missing output.
     */
    suspend fun startLiveExecution(
        command: String,
        config: ShellExecutionConfig
    ): LiveShellSession = throw UnsupportedOperationException("Live execution not supported")
    
    /**
     * Wait for a live session to complete and return the result.
     * This is platform-specific and should be implemented by each platform.
     */
    suspend fun waitForSession(
        session: LiveShellSession,
        timeoutMs: Long
    ): Int = throw UnsupportedOperationException("Session waiting not supported")
}

