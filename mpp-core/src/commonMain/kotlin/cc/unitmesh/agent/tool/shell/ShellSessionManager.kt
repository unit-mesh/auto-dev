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
     * Check if the process is still running
     */
    fun isRunning(): Boolean {
        val process = processHandle as? Process ?: return false
        return process.isAlive
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
        val process = processHandle as? Process ?: return false
        return try {
            process.destroyForcibly()
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

