package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * Ruff linter for Python
 */
class RuffLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "ruff"
    override val description = "Fast Python linter"
    override val supportedExtensions = listOf("py")

    override fun getVersionCommand() = "ruff --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "ruff check \"$filePath\" --output-format=json"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseRuffOutput(output, filePath)

    companion object {
        /**
         * Parse ruff JSON output
         * Example JSON:
         * [{"code":"F841","location":{"row":2,"column":5},"end_location":{"row":2,"column":6},"message":"Local variable `x` is assigned to but never used","url":"..."}]
         */
        fun parseRuffOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            try {
                // Normalize the output (remove newlines and extra spaces)
                val normalized = output.replace(Regex("\\s+"), " ")
                
                // Simple JSON parsing for ruff output structure
                if (normalized.trim().startsWith("[") && normalized.trim().endsWith("]")) {
                    // Split by issue objects - look for complete {} blocks
                    var depth = 0
                    var start = -1
                    val chars = normalized.toCharArray()
                    
                    for (i in chars.indices) {
                        when (chars[i]) {
                            '{' -> {
                                if (depth == 0) start = i
                                depth++
                            }
                            '}' -> {
                                depth--
                                if (depth == 0 && start != -1) {
                                    val issueJson = normalized.substring(start, i + 1)
                                    parseRuffIssue(issueJson, filePath)?.let { issues.add(it) }
                                    start = -1
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback - return empty list
            }

            return issues
        }
        
        private fun parseRuffIssue(json: String, filePath: String): LintIssue? {
            try {
                // Extract fields using regex
                val codeMatch = Regex(""""code" *: *"([^"]+)"""").find(json)
                val messageMatch = Regex(""""message" *: *"([^"]+)"""").find(json)
                
                // Extract location object - find the FIRST "location" (not "end_location")
                // Look for the pattern: "location" : { ... "row" : digit, ... "column" : digit ...}
                val locationPattern = Regex(""""location" *: *\{ *[^}]*"row" *: *(\d+)[^}]*"column" *: *(\d+)""")
                val locationMatch = locationPattern.find(json)
                
                if (codeMatch != null && messageMatch != null && locationMatch != null) {
                    return LintIssue(
                        line = locationMatch.groupValues[1].toIntOrNull() ?: 0,
                        column = locationMatch.groupValues[2].toIntOrNull() ?: 0,
                        severity = LintSeverity.WARNING,
                        message = messageMatch.groupValues[1],
                        rule = codeMatch.groupValues[1],
                        filePath = filePath
                    )
                }
            } catch (e: Exception) {
                // Skip this issue
            }
            return null
        }
    }

    override fun getInstallationInstructions() =
        "Install Ruff: pip install ruff or brew install ruff"
}