package cc.unitmesh.devti.language.compiler.exec.process

import cc.unitmesh.devti.command.InsCommand
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.process.ProcessStateManager
import cc.unitmesh.devti.process.ProcessStatus
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

/**
 * InsCommand implementation for killing processes
 */
class KillProcessInsCommand(
    private val project: Project,
    private val prop: String
) : InsCommand {

    override val commandName: BuiltinCommand = BuiltinCommand.KILL_PROCESS
    private val logger = logger<KillProcessInsCommand>()

    override suspend fun execute(): String? {
        val processStateManager = ProcessStateManager.Companion.getInstance(project)

        // Parse parameters
        val (processId, force) = parseParameters(prop)

        if (processId.isEmpty()) {
            return "Error: Process ID is required. Usage: /kill-process:process_id [--force]"
        }

        // Check if process exists
        val processInfo = processStateManager.getProcess(processId)
        if (processInfo == null) {
            return "Error: Process '$processId' not found."
        }

        // Check if process is already terminated
        if (processInfo.status != ProcessStatus.RUNNING) {
            return "Process '$processId' is already terminated (status: ${processInfo.status})."
        }

        // Kill the process
        val result = processStateManager.killProcess(processId, force)

        return if (result.success) {
            val killMethod = if (force) "forcefully killed" else "gracefully terminated"
            "Process '$processId' has been $killMethod successfully."
        } else {
            "Failed to kill process '$processId': ${result.errorMessage}"
        }
    }

    private fun parseParameters(prop: String): Pair<String, Boolean> {
        val parts = prop.trim().split(" ")
        val processId = parts.firstOrNull()?.trim() ?: ""
        val force = parts.any { it.trim() == "--force" }

        return Pair(processId, force)
    }
}