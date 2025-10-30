package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException

/**
 * JavaScript platform implementation of shell executor
 * Currently provides empty implementation - could be extended to use Node.js child_process
 */
actual class DefaultShellExecutor : ShellExecutor {

    actual override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
        // TODO: Could implement using Node.js child_process module
        throw ToolException(
            "Shell execution is not yet implemented for JavaScript platform",
            ToolErrorType.NOT_SUPPORTED
        )
    }

    actual override fun isAvailable(): Boolean = false

    actual override fun getDefaultShell(): String? = null
}
