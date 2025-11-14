package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * Ruff linter for Python
 */
class RuffLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "ruff"
    override val description = "Fast Python linter"
    override val supportedExtensions = listOf("py")

    override fun getVersionCommand() = "ruff --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "ruff check \"$filePath\" --output-format=json"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse ruff JSON output
        // Simplified parsing - real implementation would use JSON parser
        try {
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"code\"")) {
                    issues.add(
                        LintIssue(
                            line = 0,
                            column = 0,
                            severity = LintSeverity.WARNING,
                            message = "Ruff issue found",
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
        "Install Ruff: pip install ruff or brew install ruff"
}