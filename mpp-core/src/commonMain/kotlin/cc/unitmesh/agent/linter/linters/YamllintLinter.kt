package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class YamllintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "yamllint"
    override val description = "YAML linter"
    override val supportedExtensions = listOf("yaml", "yml")

    override fun getVersionCommand() = "yamllint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "yamllint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseYamllintOutput(output, filePath)

    companion object {
        /**
         * Parse yamllint output format
         * Example:
         * bad.yaml
         *   4:25      error    trailing spaces  (trailing-spaces)
         *   19:10     warning  truthy value should be one of [false, true]  (truthy)
         */
        fun parseYamllintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // yamllint format: line:column  level  message  (rule)
            val pattern = Regex("""^\s*(\d+):(\d+)\s+(error|warning)\s+(.+?)\s+\(([^)]+)\)\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line)
                if (match != null) {
                    val (lineNum, col, level, message, rule) = match.destructured

                    val severity = when (level.lowercase()) {
                        "error" ->cc.unitmesh.agent.linter.LintSeverity.ERROR
                        "warning" ->cc.unitmesh.agent.linter.LintSeverity.WARNING
                        else ->cc.unitmesh.agent.linter.LintSeverity.INFO
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
        "Install yamllint: pip install yamllint"
}

