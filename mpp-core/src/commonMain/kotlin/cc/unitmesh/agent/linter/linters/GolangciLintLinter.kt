package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class GolangciLintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "golangci-lint"
    override val description = "Fast linters runner for Go"
    override val supportedExtensions = listOf("go")

    override fun getVersionCommand() = "golangci-lint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "golangci-lint run \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseGolangciLintOutput(output, filePath)

    companion object {
        /**
         * Parse golangci-lint output format
         * Example: bad_go.go:5:2: "os" imported and not used (typecheck)
         */
        fun parseGolangciLintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // golangci-lint format: filepath:line:column: message (rule)
            val pattern = Regex("""^(.+?):(\d+):(\d+):\s*(.+?)\s*\(([^)]+)\)\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (path, lineNum, col, message, rule) = match.destructured

                    // golangci-lint doesn't specify severity in text output, use WARNING as default
                    val severity = when {
                        message.contains("error", ignoreCase = true) -> LintSeverity.ERROR
                        else -> LintSeverity.WARNING
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
        "Install golangci-lint: https://golangci-lint.run/welcome/install/"
}

