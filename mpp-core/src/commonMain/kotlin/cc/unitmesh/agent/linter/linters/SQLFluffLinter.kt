package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class SQLFluffLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "sqlfluff"
    override val description = "SQL linter and formatter"
    override val supportedExtensions = listOf("sql")

    override fun getVersionCommand() = "sqlfluff --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "sqlfluff lint \"$filePath\" --dialect ansi"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseSQLFluffOutput(output, filePath)

    companion object {
        /**
         * Parse sqlfluff output format
         * Example: L:   3 | P:   1 | AM04 | Query produces an unknown number of result columns.
         */
        fun parseSQLFluffOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // sqlfluff format: L: line | P: column | code | message
            val pattern = Regex("""^L:\s*(\d+)\s*\|\s*P:\s*(\d+)\s*\|\s*(\S+)\s*\|\s*(.+?)\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (lineNum, col, code, message) = match.destructured

                    // sqlfluff doesn't specify severity in text output, use WARNING as default
                    val severity = when {
                        code == "PRS" -> LintSeverity.ERROR  // Parse errors are severe
                        message.contains("error", ignoreCase = true) -> LintSeverity.ERROR
                        else -> LintSeverity.WARNING
                    }

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = col.toIntOrNull() ?: 0,
                            severity = severity,
                            message = message.trim(),
                            rule = code,
                            filePath = filePath
                        )
                    )
                }
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install SQLFluff: pip install sqlfluff"
}

