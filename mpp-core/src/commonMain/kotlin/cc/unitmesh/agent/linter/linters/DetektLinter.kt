package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class DetektLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "detekt"
    override val description = "Static code analysis for Kotlin"
    override val supportedExtensions = listOf("kt", "kts")

    override fun getVersionCommand() = "detekt --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "detekt --input \"$filePath\" --report txt:stdout"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse detekt output format
        // Example: File.kt:10:5: warning: Line is too long
        val pattern = Regex("""(.+):(\d+):(\d+):\s*(error|warning|info):\s*(.+)""")

        for (line in output.lines()) {
            val match = pattern.find(line)
            if (match != null) {
                val (_, lineNum, col, severityStr, message) = match.destructured

                val severity = when (severityStr.lowercase()) {
                    "error" -> LintSeverity.ERROR
                    "warning" -> LintSeverity.WARNING
                    else -> LintSeverity.INFO
                }

                issues.add(
                    LintIssue(
                        line = lineNum.toIntOrNull() ?: 0,
                        column = col.toIntOrNull() ?: 0,
                        severity = severity,
                        message = message,
                        rule = null,
                        filePath = filePath
                    )
                )
            }
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install Detekt: Add to build.gradle.kts or download CLI from https://github.com/detekt/detekt"
}