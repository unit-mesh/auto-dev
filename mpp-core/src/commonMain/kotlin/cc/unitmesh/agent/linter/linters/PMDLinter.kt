package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * PMD linter for Java
 */
class PMDLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "pmd"
    override val description = "Source code analyzer for Java and other languages"
    override val supportedExtensions = listOf("java")

    override fun getVersionCommand() = "pmd --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "pmd check -d \"$filePath\" -f text -R rulesets/java/quickstart.xml"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        val issues = mutableListOf<LintIssue>()

        // Parse PMD output format
        // Example: /path/to/File.java:10:	Rule violation message
        val pattern = Regex("""(.+):(\d+):\s*(.+)""")

        for (line in output.lines()) {
            val match = pattern.find(line)
            if (match != null) {
                val (_, lineNum, message) = match.destructured

                // PMD doesn't always specify severity in text format, default to WARNING
                val severity = when {
                    message.contains("error", ignoreCase = true) -> LintSeverity.ERROR
                    else -> LintSeverity.WARNING
                }

                issues.add(
                    LintIssue(
                        line = lineNum.toIntOrNull() ?: 0,
                        column = 0,
                        severity = severity,
                        message = message.trim(),
                        rule = null,
                        filePath = filePath
                    )
                )
            }
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install PMD: brew install pmd or download from https://pmd.github.io/"
}