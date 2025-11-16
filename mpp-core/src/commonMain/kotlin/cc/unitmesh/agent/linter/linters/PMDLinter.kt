package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
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

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parsePMDOutput(output, filePath)

    companion object {
        /**
         * Parse PMD output format
         * Example: File.java:10: RuleName: Rule violation message
         * Example: File.java:10:5: RuleName: Rule violation message
         */
        fun parsePMDOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // PMD format: filename:line: RuleName: message or filename:line:column: RuleName: message
            val pattern = Regex("""^([^:]+):(\d+):(?:(\d+):)?\s*([^:]+):\s*(.+)$""")

            for (line in output.lines()) {
                // Skip warning lines
                if (line.trim().startsWith("[")) {
                    continue
                }
                
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, colStr, rule, message) = match.destructured

                    // PMD doesn't specify severity in text format, default to WARNING
                    val severity = when {
                        message.contains("error", ignoreCase = true) ->cc.unitmesh.agent.linter.LintSeverity.ERROR
                        else ->cc.unitmesh.agent.linter.LintSeverity.WARNING
                    }

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = colStr.toIntOrNull() ?: 0,
                            severity = severity,
                            message = message.trim(),
                            rule = rule.trim(),
                            filePath = filePath
                        )
                    )
                }
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install PMD: brew install pmd or download from https://pmd.github.io/"
}