package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class PylintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "pylint"
    override val description = "Python code static checker"
    override val supportedExtensions = listOf("py")

    override fun getVersionCommand() = "pylint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "pylint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parsePylintOutput(output, filePath)

    companion object {
        /**
         * Parse pylint output format
         * Example: bad.py:18:0: C0303: Trailing whitespace (trailing-whitespace)
         */
        fun parsePylintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // pylint format: file.py:line:column: code: message (rule-name)
            val pattern = Regex("""^(.+?):(\d+):(\d+):\s*([CRWEF]\d+):\s*(.+?)\s*\(([^)]+)\)\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, col, code, message, rule) = match.destructured

                    // Pylint uses letter prefixes: C=Convention, R=Refactor, W=Warning, E=Error, F=Fatal
                    val severity = when (code.firstOrNull()) {
                        'E', 'F' -> LintSeverity.ERROR
                        'W' -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
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
        "Install Pylint: pip install pylint"
}

