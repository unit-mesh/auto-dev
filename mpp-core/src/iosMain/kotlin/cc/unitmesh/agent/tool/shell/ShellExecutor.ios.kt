package cc.unitmesh.agent.tool.shell

/**
 * iOS implementation of DefaultShellExecutor
 * Shell execution is not supported on iOS
 */
actual class DefaultShellExecutor : ShellExecutor {
    actual override suspend fun execute(
        command: String,
        config: ShellExecutionConfig
    ): ShellResult {
        // iOS: Shell execution is not supported
        return ShellResult(
            exitCode = -1,
            stdout = "",
            stderr = "Shell execution is not supported on iOS",
            command = command,
            executionTimeMs = 0
        )
    }

    actual override fun isAvailable(): Boolean {
        return false
    }

    actual override fun getDefaultShell(): String? {
        return null
    }
}

