package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class ActionlintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "actionlint"
    override val description = "GitHub Actions workflow linter"
    override val supportedExtensions = listOf("yml", "yaml")

    override fun getVersionCommand() = "actionlint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "actionlint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseActionlintOutput(output, filePath)

    companion object {
        /**
         * Parse actionlint output format
         * Example: bad-workflow.yml:5:5: unexpected key "branch" for "push" section [syntax-check]
         */
        fun parseActionlintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // actionlint format: filename:line:column: message [rule]
            val pattern = Regex("""^(.+?):(\d+):(\d+):\s+(.+?)\s+\[([^\]]+)\]\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, col, message, rule) = match.destructured

                    // actionlint treats most issues as errors
                    val severity = when {
                        message.contains("deprecated", ignoreCase = true) -> LintSeverity.WARNING
                        message.contains("obsolete", ignoreCase = true) -> LintSeverity.WARNING
                        message.contains("too old", ignoreCase = true) -> LintSeverity.WARNING
                        else -> LintSeverity.ERROR
                    }

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = col.toIntOrNull() ?: 0,
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
        "Install actionlint: https://github.com/rhysd/actionlint#installation"
}

