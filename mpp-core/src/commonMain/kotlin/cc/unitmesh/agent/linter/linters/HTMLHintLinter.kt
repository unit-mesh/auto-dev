package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class HTMLHintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "htmlhint"
    override val description = "HTML linter"
    override val supportedExtensions = listOf("html", "htm")

    override fun getVersionCommand() = "htmlhint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "htmlhint \"$filePath\" --no-color"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseHTMLHintOutput(output, filePath)

    companion object {
        /**
         * Parse htmlhint output format
         * Example:
         *    L10 |        <p>This paragraph is not closed
         *               ^ Tag must be paired, missing: [ </p> ], start tag match failed [ <p> ] on line 10. (tag-pair)
         */
        fun parseHTMLHintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // HTMLHint format: L{line} | ... followed by ^ message (rule)
            val linePattern = Regex("""^\s*L(\d+)\s*\|""")
            val errorPattern = Regex("""^\s*\^\s*(.+?)\s*\(([^)]+)\)\s*$""")

            val lines = output.lines()
            var i = 0
            while (i < lines.size) {
                val line = lines[i]
                val lineMatch = linePattern.find(line)
                
                if (lineMatch != null) {
                    val lineNum = lineMatch.groupValues[1].toIntOrNull() ?: 0
                    
                    // Look for the error message in next few lines
                    for (j in i + 1 until minOf(i + 5, lines.size)) {
                        val errorMatch = errorPattern.find(lines[j])
                        if (errorMatch != null) {
                            val message = errorMatch.groupValues[1].trim()
                            val rule = errorMatch.groupValues[2]
                            
                            issues.add(
                                LintIssue(
                                    line = lineNum,
                                    column = 1, // HTMLHint doesn't provide column in text output
                                    severity =cc.unitmesh.agent.linter.LintSeverity.ERROR,
                                    message = message,
                                    rule = rule,
                                    filePath = filePath
                                )
                            )
                            i = j
                            break
                        }
                    }
                }
                i++
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install HTMLHint: npm install -g htmlhint"
}

