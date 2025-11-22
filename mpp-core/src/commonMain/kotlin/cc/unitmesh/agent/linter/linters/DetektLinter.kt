package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class DetektLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "detekt"
    override val description = "Static code analysis for Kotlin"
    override val supportedExtensions = listOf("kt", "kts")

    override fun getVersionCommand() = "detekt --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "detekt --input \"$filePath\" --report txt:stdout"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> = parseDetektOutput(output, filePath)

    companion object {
        /**
         * Parse detekt output format
         * Example: /path/to/File.kt:10:5: Line is too long [RuleName]
         */
        fun parseDetektOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            val pattern = Regex("""^(.+?):(\d+):(\d+):\s*(.+?)\s*\[([^\]]+)\]\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (path, lineNum, col, message, rule) = match.destructured

                    val severity = when {
                        message.contains("error", ignoreCase = true) ->cc.unitmesh.agent.linter.LintSeverity.ERROR
                        else ->cc.unitmesh.agent.linter.LintSeverity.WARNING
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
        "Install Detekt: Add to build.gradle.kts or download CLI from https://github.com/detekt/detekt"
}