package cc.unitmesh.agent.tool.shell

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolException

/**
 * WebAssembly platform implementation of shell executor
 * Shell execution is not supported in WebAssembly environment
 */
actual class DefaultShellExecutor : ShellExecutor {

    actual override suspend fun execute(command: String, config: ShellExecutionConfig): ShellResult {
        throw ToolException(
            "Shell execution is not supported in WebAssembly environment",
            ToolErrorType.NOT_SUPPORTED
        )
    }

    actual override fun isAvailable(): Boolean = false

    actual override fun getDefaultShell(): String? = null
}
