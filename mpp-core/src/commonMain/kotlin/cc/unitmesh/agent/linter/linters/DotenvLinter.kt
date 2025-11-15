package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class DotenvLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "dotenv-linter"
    override val description = "Lightning-fast linter for .env files"
    override val supportedExtensions = listOf("env")

    override fun getVersionCommand() = "dotenv-linter --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "dotenv-linter check \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseDotenvLinterOutput(output, filePath)

    companion object {
        /**
         * Parse dotenv-linter output format
         * Example: bad.env:5 DuplicatedKey: The API_KEY key is duplicated
         */
        fun parseDotenvLinterOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // dotenv-linter format: filename:line ruleName: message
            val pattern = Regex("""^(.+?):(\d+)\s+(\w+):\s+(.+)$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, rule, message) = match.destructured

                    // Determine severity based on rule type
                    val severity = when (rule) {
                        "DuplicatedKey", "LowercaseKey", "IncorrectDelimiter" -> LintSeverity.ERROR
                        else -> LintSeverity.WARNING
                    }

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = 1, // dotenv-linter doesn't provide column info
                            severity = severity,
                            message = message.trim(),
                            rule = rule,
                            filePath = filePath
                        )
                    )
                }
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install dotenv-linter: https://dotenv-linter.github.io/#/installation"
}

