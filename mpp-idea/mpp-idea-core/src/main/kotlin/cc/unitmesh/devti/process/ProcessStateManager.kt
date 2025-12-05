package cc.unitmesh.devti.process

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Service for managing process states and lifecycle
 */
@Service(Service.Level.PROJECT)
class ProcessStateManager(private val project: Project) {
    private val logger = logger<ProcessStateManager>()
    private val processes = ConcurrentHashMap<String, ProcessInfo>()
    private val processHandlers = ConcurrentHashMap<String, ProcessHandler>()
    private val processInputStreams = ConcurrentHashMap<String, OutputStream>()
    private val outputStorage = ProcessOutputStorage()
    private val processIdCounter = AtomicLong(0)
    
    /**
     * Generate a unique process ID
     */
    fun generateProcessId(): String {
        return "proc_${System.currentTimeMillis()}_${processIdCounter.incrementAndGet()}"
    }
    
    /**
     * Register a new process
     */
    fun registerProcess(processInfo: ProcessInfo, processHandler: ProcessHandler? = null, inputStream: OutputStream? = null) {
        processes[processInfo.processId] = processInfo
        processHandler?.let { processHandlers[processInfo.processId] = it }
        inputStream?.let { processInputStreams[processInfo.processId] = it }
        
        logger.info("Registered process: ${processInfo.processId} - ${processInfo.command}")
    }
    
    /**
     * Update process status
     */
    fun updateProcessStatus(processId: String, status: ProcessStatus, exitCode: Int? = null) {
        processes[processId]?.let { info ->
            val updatedInfo = info.copy(
                status = status,
                exitCode = exitCode,
                endTime = if (status in listOf(ProcessStatus.COMPLETED, ProcessStatus.FAILED, ProcessStatus.KILLED, ProcessStatus.TIMED_OUT)) 
                    System.currentTimeMillis() else info.endTime
            )
            processes[processId] = updatedInfo
            
            logger.info("Updated process status: $processId -> $status (exit code: $exitCode)")
            
            // Clean up resources for terminated processes
            if (status in listOf(ProcessStatus.COMPLETED, ProcessStatus.FAILED, ProcessStatus.KILLED, ProcessStatus.TIMED_OUT)) {
                cleanupProcess(processId)
            }
        }
    }
    
    /**
     * Get process information by ID
     */
    fun getProcess(processId: String): ProcessInfo? = processes[processId]
    
    /**
     * Get all processes
     */
    fun getAllProcesses(includeTerminated: Boolean = false): List<ProcessInfo> {
        return if (includeTerminated) {
            processes.values.toList()
        } else {
            processes.values.filter { it.status == ProcessStatus.RUNNING }.toList()
        }
    }
    
    /**
     * Get process handler by ID
     */
    fun getProcessHandler(processId: String): ProcessHandler? = processHandlers[processId]
    
    /**
     * Get process input stream by ID
     */
    fun getProcessInputStream(processId: String): OutputStream? = processInputStreams[processId]
    
    /**
     * Kill a process
     */
    fun killProcess(processId: String, force: Boolean = false): KillProcessResponse {
        val processHandler = processHandlers[processId]
        if (processHandler == null) {
            return KillProcessResponse(false, "Process not found or already terminated")
        }
        
        return try {
            if (force) {
                processHandler.destroyProcess()
            } else {
                processHandler.detachProcess()
            }
            
            updateProcessStatus(processId, ProcessStatus.KILLED)
            KillProcessResponse(true)
        } catch (e: Exception) {
            logger.warn("Failed to kill process $processId", e)
            KillProcessResponse(false, "Failed to kill process: ${e.message}")
        }
    }
    
    /**
     * Write input to a process
     */
    fun writeProcessInput(processId: String, inputData: String, appendNewline: Boolean = true): WriteProcessInputResponse {
        val inputStream = processInputStreams[processId]
        if (inputStream == null) {
            return WriteProcessInputResponse(false, "Process not found or input stream not available")
        }
        
        return try {
            val dataToWrite = if (appendNewline && !inputData.endsWith("\n")) {
                inputData + "\n"
            } else {
                inputData
            }
            
            inputStream.write(dataToWrite.toByteArray())
            inputStream.flush()
            
            WriteProcessInputResponse(true)
        } catch (e: Exception) {
            logger.warn("Failed to write input to process $processId", e)
            WriteProcessInputResponse(false, "Failed to write input: ${e.message}")
        }
    }
    
    /**
     * Read process output
     */
    fun readProcessOutput(processId: String, includeStdout: Boolean = true, includeStderr: Boolean = true, maxBytes: Int = 10000): ReadProcessOutputResponse {
        val stdout = if (includeStdout) outputStorage.getStdout(processId, maxBytes) else ""
        val stderr = if (includeStderr) outputStorage.getStderr(processId, maxBytes) else ""
        
        val hasMoreStdout = if (includeStdout) outputStorage.hasMoreStdout(processId, maxBytes) else false
        val hasMoreStderr = if (includeStderr) outputStorage.hasMoreStderr(processId, maxBytes) else false
        
        return ReadProcessOutputResponse(
            stdout = stdout,
            stderr = stderr,
            hasMore = hasMoreStdout || hasMoreStderr
        )
    }
    
    /**
     * Append stdout data for a process
     */
    fun appendStdout(processId: String, data: String) {
        outputStorage.appendStdout(processId, data)
    }
    
    /**
     * Append stderr data for a process
     */
    fun appendStderr(processId: String, data: String) {
        outputStorage.appendStderr(processId, data)
    }
    
    /**
     * Remove a process from management
     */
    fun removeProcess(processId: String) {
        processes.remove(processId)
        cleanupProcess(processId)
        logger.info("Removed process: $processId")
    }
    
    /**
     * Clean up process resources
     */
    private fun cleanupProcess(processId: String) {
        processHandlers.remove(processId)
        processInputStreams.remove(processId)?.let { stream ->
            try {
                stream.close()
            } catch (e: Exception) {
                logger.warn("Failed to close input stream for process $processId", e)
            }
        }
        
        // Keep output for a while for debugging, but could be cleaned up later
        // outputStorage.clearOutput(processId)
    }
    
    /**
     * Get running processes count
     */
    fun getRunningProcessesCount(): Int {
        return processes.values.count { it.status == ProcessStatus.RUNNING }
    }
    
    /**
     * Check if a process is running
     */
    fun isProcessRunning(processId: String): Boolean {
        return processes[processId]?.status == ProcessStatus.RUNNING
    }
    
    companion object {
        @JvmStatic
        fun getInstance(project: Project): ProcessStateManager {
            return project.getService(ProcessStateManager::class.java)
        }
    }
}
