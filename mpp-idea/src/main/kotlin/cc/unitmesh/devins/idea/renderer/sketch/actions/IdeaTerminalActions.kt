package cc.unitmesh.devins.idea.renderer.sketch.actions

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.run.ProcessExecutor
import cc.unitmesh.devti.sketch.run.ShellSafetyCheck
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.jetbrains.ide.PooledThreadExecutor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Business logic actions for Terminal operations in mpp-idea.
 * Reuses core module's ShellSafetyCheck, ProcessExecutor logic.
 * 
 * Related GitHub Issue: https://github.com/phodal/auto-dev/issues/25
 */
object IdeaTerminalActions {
    
    /**
     * Check if a command is dangerous
     * @return Pair of (isDangerous, reason)
     */
    fun checkDangerousCommand(command: String): Pair<Boolean, String> {
        return try {
            ShellSafetyCheck.checkDangerousCommand(command)
        } catch (e: Exception) {
            Pair(true, "Error checking command safety: ${e.message}")
        }
    }
    
    /**
     * Execute a shell command and return the result
     */
    suspend fun executeCommand(
        project: Project,
        command: String,
        dispatcher: CoroutineDispatcher = PooledThreadExecutor.INSTANCE.asCoroutineDispatcher()
    ): ExecutionResult {
        val (isDangerous, reason) = checkDangerousCommand(command)
        if (isDangerous) {
            return ExecutionResult(
                exitCode = -1,
                output = "",
                error = "Command blocked for safety: $reason",
                isDangerous = true,
                dangerReason = reason
            )
        }
        
        return try {
            val executor = project.getService(ProcessExecutor::class.java)
            val result = executor.executeCode(command, dispatcher)
            ExecutionResult(
                exitCode = result.exitCode,
                output = result.stdOutput,
                error = result.errOutput,
                isDangerous = false,
                dangerReason = ""
            )
        } catch (e: Exception) {
            ExecutionResult(
                exitCode = -1,
                output = "",
                error = "Execution error: ${e.message}",
                isDangerous = false,
                dangerReason = ""
            )
        }
    }
    
    /**
     * Execute command synchronously (blocking)
     */
    fun executeCommandSync(project: Project, command: String): ExecutionResult {
        val (isDangerous, reason) = checkDangerousCommand(command)
        if (isDangerous) {
            return ExecutionResult(
                exitCode = -1,
                output = "",
                error = "Command blocked for safety: $reason",
                isDangerous = true,
                dangerReason = reason
            )
        }
        
        return try {
            val executor = project.getService(ProcessExecutor::class.java)
            val result = executor.executeCode(command)
            ExecutionResult(
                exitCode = result.exitCode,
                output = result.stdOutput,
                error = result.errOutput,
                isDangerous = false,
                dangerReason = ""
            )
        } catch (e: Exception) {
            ExecutionResult(
                exitCode = -1,
                output = "",
                error = "Execution error: ${e.message}",
                isDangerous = false,
                dangerReason = ""
            )
        }
    }
    
    /**
     * Copy command or output to clipboard
     */
    fun copyToClipboard(text: String): Boolean {
        return try {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, null)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Notify user about execution result
     */
    fun notifyResult(project: Project, result: ExecutionResult) {
        if (result.exitCode == 0) {
            AutoDevNotifications.notify(project, "Command executed successfully")
        } else if (result.isDangerous) {
            AutoDevNotifications.warn(project, "Command blocked: ${result.dangerReason}")
        } else {
            AutoDevNotifications.error(project, "Command failed with exit code ${result.exitCode}")
        }
    }
}

/**
 * Result of command execution
 */
data class ExecutionResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val isDangerous: Boolean,
    val dangerReason: String
) {
    val isSuccess: Boolean get() = exitCode == 0 && !isDangerous
    val displayOutput: String get() = if (output.isNotBlank()) output else error
}

