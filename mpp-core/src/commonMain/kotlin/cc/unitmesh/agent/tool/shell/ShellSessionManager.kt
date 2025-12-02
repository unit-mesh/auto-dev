package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.Platform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages all active shell sessions.
 * Provides centralized access to running processes for read, wait, and kill operations.
 * 
 * Design inspired by Augment's launch-process/read-process/kill-process pattern.
 */
object ShellSessionManager {
    
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, ManagedSession>()
    
    private val _activeSessions = MutableStateFlow<List<String>>(emptyList())
    val activeSessions: StateFlow<List<String>> = _activeSessions.asStateFlow()
    
    /**
     * Register a new session
     */
    suspend fun registerSession(
        sessionId: String,
        command: String,
        workingDirectory: String?,
        processHandle: Any?,
        startTime: Long = Platform.getCurrentTimestamp()
    ): ManagedSession {
        val session = ManagedSession(
            sessionId = sessionId,
            command = command,
            workingDirectory = workingDirectory,
            processHandle = processHandle,
            startTime = startTime
        )
        
        mutex.withLock {
            sessions[sessionId] = session
            _activeSessions.value = sessions.keys.toList()
        }
        
        return session
    }
    
    /**
     * Get a session by ID
     */
    suspend fun getSession(sessionId: String): ManagedSession? {
        return mutex.withLock { sessions[sessionId] }
    }

    /**
     * Mark a session as cancelled by user.
     * This is a non-suspend function for use in UI callbacks where coroutine context may not be available.
     * Note: This accesses the sessions map without locking, which is safe for this specific use case
     * because we're only setting a boolean flag on an existing session.
     */
    fun markSessionCancelledByUser(sessionId: String) {
        sessions[sessionId]?.cancelledByUser = true
    }

    /**
     * Get all active (running) sessions
     */
    suspend fun getActiveSessions(): List<ManagedSession> {
        return mutex.withLock { 
            sessions.values.filter { it.isRunning() }.toList() 
        }
    }
    
    /**
     * Remove a session (called when process completes or is killed)
     */
    suspend fun removeSession(sessionId: String): ManagedSession? {
        return mutex.withLock {
            val removed = sessions.remove(sessionId)
            _activeSessions.value = sessions.keys.toList()
            removed
        }
    }
    
    /**
     * Clear all sessions (for cleanup)
     */
    suspend fun clearAll() {
        mutex.withLock {
            sessions.values.forEach { it.kill() }
            sessions.clear()
            _activeSessions.value = emptyList()
        }
    }
}

/**
 * Represents a managed shell session with output buffering and state tracking.
 */
class ManagedSession(
    val sessionId: String,
    val command: String,
    val workingDirectory: String?,
    val processHandle: Any?,
    val startTime: Long
) {
    private val outputBuffer = StringBuilder()
    private val mutex = Mutex()

    private var _exitCode: Int? = null
    private var _endTime: Long? = null

    val exitCode: Int? get() = _exitCode
    val endTime: Long? get() = _endTime

    /**
     * Flag to indicate if the session was cancelled by user.
     * This helps distinguish between user cancellation (exit code 137) and other failures.
     */
    var cancelledByUser: Boolean = false

    // Callbacks for platform-specific process operations
    private var isAliveChecker: (() -> Boolean)? = null
    private var killHandler: (() -> Unit)? = null

    /**
     * Set platform-specific process handlers
     */
    fun setProcessHandlers(isAlive: () -> Boolean, kill: () -> Unit) {
        isAliveChecker = isAlive
        killHandler = kill
    }

    /**
     * Check if the process is still running
     */
    fun isRunning(): Boolean {
        // If we have an exit code, the process has completed
        if (_exitCode != null) return false
        // Use the platform-specific checker if available
        return isAliveChecker?.invoke() ?: false
    }

    /**
     * Get current output (thread-safe)
     */
    suspend fun getOutput(): String {
        return mutex.withLock { outputBuffer.toString() }
    }

    /**
     * Append output (called by output collector)
     */
    suspend fun appendOutput(text: String) {
        mutex.withLock { outputBuffer.append(text) }
    }

    /**
     * Mark session as completed
     */
    fun markCompleted(exitCode: Int, endTime: Long = Platform.getCurrentTimestamp()) {
        _exitCode = exitCode
        _endTime = endTime
    }

    /**
     * Kill the process
     */
    fun kill(): Boolean {
        return try {
            killHandler?.invoke()
            markCompleted(-1)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get execution time in milliseconds
     */
    fun getExecutionTimeMs(): Long {
        val end = _endTime ?: Platform.getCurrentTimestamp()
        return end - startTime
    }
}

