package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class SemgrepLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "semgrep"
    override val description = "Static analysis tool for finding bugs and security issues"
    override val supportedExtensions = listOf("py", "js", "ts", "java", "go", "rb", "php", "c", "cpp", "yaml", "json")

    override fun getVersionCommand() = "semgrep --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "semgrep --config=auto --text \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseSemgrepOutput(output, filePath)

    companion object {
        /**
         * Parse semgrep text output format
         * Example:
         *     insecure.py
         *     ❯❱ python.lang.security.deserialization.pickle.avoid-pickle
         *           Avoid using `pickle`...
         *            11┆ return pickle.loads(data)
         */
        fun parseSemgrepOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()
            
            val lines = output.lines()
            var i = 0
            
            while (i < lines.size) {
                val line = lines[i]
                
                // Look for rule ID (starts with ❯❱)
                if (line.trim().startsWith("❯❱") || line.trim().startsWith(">>")) {
                    val ruleId = line.trim().removePrefix("❯❱").removePrefix(">>").trim()
                    var message = ""
                    var lineNum = 0
                    
                    // Look ahead for message and line number
                    for (j in i + 1 until minOf(i + 15, lines.size)) {
                        val nextLine = lines[j].trim()
                        
                        // Extract line number (format: "11┆ code" or "11| code")
                        val linePattern = Regex("""^(\d+)[┆|]\s*(.+)$""")
                        val lineMatch = linePattern.find(nextLine)
                        
                        if (lineMatch != null) {
                            lineNum = lineMatch.groupValues[1].toIntOrNull() ?: 0
                            break
                        }
                        
                        // Collect message lines (but skip decorative lines)
                        if (nextLine.isNotEmpty() && 
                            !nextLine.startsWith("───") && 
                            !nextLine.startsWith("Details:") &&
                            !nextLine.matches(Regex("""^\d+[┆|].*"""))) {
                            if (message.isNotEmpty()) message += " "
                            message += nextLine
                        }
                    }
                    
                    if (lineNum > 0) {
                        issues.add(
                            LintIssue(
                                line = lineNum,
                                column = 1, // Semgrep text output doesn't provide column
                                severity = LintSeverity.WARNING, // Semgrep findings are typically warnings
                                message = message.ifEmpty { "Security or code quality issue detected" },
                                rule = ruleId,
                                filePath = filePath
                            )
                        )
                    }
                    i += 10 // Skip ahead to avoid re-parsing
                } else {
                    i++
                }
            }
            
            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install Semgrep: pip3 install semgrep or brew install semgrep"
}

