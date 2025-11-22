package cc.unitmesh.agent.linter

import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.logging.getLogger

/**
 * Base class for linters that run as shell commands
 */
abstract class ShellBasedLinter(private val shellExecutor: ShellExecutor) : Linter {

    private val logger = getLogger("ShellBasedLinter")

    /**
     * Get the command to check if linter is installed
     */
    abstract fun getVersionCommand(): String

    /**
     * Get the command to lint a file
     */
    abstract fun getLintCommand(filePath: String, projectPath: String): String

    /**
     * Parse the output of the linter into LintIssues
     */
    abstract fun parseOutput(output: String, filePath: String): List<LintIssue>

    override suspend fun isAvailable(): Boolean {
        logger.info { "Shell executor: ${shellExecutor::class.simpleName}, getVersionCommand: ${getVersionCommand()}" }
        return try {
            val config = ShellExecutionConfig(timeoutMs = 5000L)
            val result = shellExecutor.execute(getVersionCommand(), config)
            result.exitCode == 0
        } catch (e: Exception) {
            logger.warn { "Linter $name not available: ${e.message}" }
            false
        }
    }

    override suspend fun lintFile(filePath: String, projectPath: String): LintResult {
        try {
            if (!isAvailable()) {
                return LintResult(
                    filePath = filePath,
                    issues = emptyList(),
                    success = false,
                    errorMessage = "Linter $name is not installed. ${getInstallationInstructions()}",
                    linterName = name
                )
            }

            val command = getLintCommand(filePath, projectPath)
            logger.info { "Running linter: $command" }

            val config = ShellExecutionConfig(
                workingDirectory = projectPath,
                timeoutMs = 30000L
            )
            val result = shellExecutor.execute(command, config)

            // Many linters return non-zero exit code when issues are found
            // So we consider it successful if we can parse the output
            val issues = parseOutput(result.stdout, filePath)

            return LintResult(
                filePath = filePath,
                issues = issues,
                success = true,
                errorMessage = if (result.exitCode != 0 && issues.isEmpty()) result.stderr else null,
                linterName = name
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to lint file $filePath with $name" }
            return LintResult(
                filePath = filePath,
                issues = emptyList(),
                success = false,
                errorMessage = "Failed to run linter: ${e.message}",
                linterName = name
            )
        }
    }
}

