package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class MarkdownlintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "markdownlint"
    override val description = "Markdown linter and style checker"
    override val supportedExtensions = listOf("md", "markdown")

    override fun getVersionCommand() = "markdownlint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "markdownlint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseMarkdownlintOutput(output, filePath)

    companion object {
        /**
         * Parse markdownlint output format
         * Example: bad.md:5 MD022/blanks-around-headings Headings should be surrounded by blank lines
         * Example with column: bad.md:18:81 MD013/line-length Line length [Expected: 80; Actual: 148]
         */
        fun parseMarkdownlintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // markdownlint format: filename:line[:column] code/name description [details]
            val pattern = Regex("""^(.+?):(\d+)(?::(\d+))?\s+(\S+)\s+(.+)$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, col, code, message) = match.destructured

                    // Extract rule code (before the slash)
                    val ruleCode = code.split("/").firstOrNull() ?: code

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = col.toIntOrNull() ?: 1,
                            severity = LintSeverity.WARNING, // markdownlint treats everything as warnings
                            message = message.trim(),
                            rule = ruleCode,
                            filePath = filePath
                        )
                    )
                }
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install markdownlint-cli: npm install -g markdownlint-cli"
}

