package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * ShellCheck linter for shell scripts
 */
class ShellCheckLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "shellcheck"
    override val description = "Static analysis tool for shell scripts"
    override val supportedExtensions = listOf("sh", "bash")

    override fun getVersionCommand() = "shellcheck --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "shellcheck -f json \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse shellcheck JSON output
        try {
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"level\"")) {
                    val severity = when {
                        line.contains("error") -> LintSeverity.ERROR
                        line.contains("warning") -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    }

                    issues.add(
                        LintIssue(
                            line = 0,
                            column = 0,
                            severity = severity,
                            message = "ShellCheck issue found",
                            rule = null,
                            filePath = filePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install ShellCheck: brew install shellcheck or apt-get install shellcheck"
}