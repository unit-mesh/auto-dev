package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * ShellCheck linter for shell scripts
 */
class ShellCheckLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "shellcheck"
    override val description = "Static analysis tool for shell scripts"
    override val supportedExtensions = listOf("sh", "bash")

    override fun getVersionCommand() = "shellcheck --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "shellcheck -f json \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseShellCheckOutput(output, filePath)

    companion object {
        /**
         * Parse shellcheck JSON output
         * Example JSON:
         * [{"file":"script.sh","line":6,"column":1,"level":"warning","code":2034,"message":"Variable appears unused."}]
         */
        fun parseShellCheckOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            try {
                // Simple JSON parsing for shellcheck output structure
                if (output.trim().startsWith("[") && output.trim().endsWith("]")) {
                    // Extract individual issue objects
                    val jsonPattern = Regex("""\{[^}]*"level"[^}]*\}""")
                    val matches = jsonPattern.findAll(output)

                    for (match in matches) {
                        val json = match.value
                        
                        // Extract fields using regex
                        val lineMatch = Regex(""""line"\s*:\s*(\d+)""").find(json)
                        val colMatch = Regex(""""column"\s*:\s*(\d+)""").find(json)
                        val levelMatch = Regex(""""level"\s*:\s*"([^"]+)"""").find(json)
                        val codeMatch = Regex(""""code"\s*:\s*(\d+)""").find(json)
                        val messageMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(json)

                        if (lineMatch != null && messageMatch != null && levelMatch != null) {
                            val severity = when (levelMatch.groupValues[1].lowercase()) {
                                "error" ->cc.unitmesh.agent.linter.LintSeverity.ERROR
                                "warning" ->cc.unitmesh.agent.linter.LintSeverity.WARNING
                                else ->cc.unitmesh.agent.linter.LintSeverity.INFO
                            }

                            issues.add(
                                LintIssue(
                                    line = lineMatch.groupValues[1].toIntOrNull() ?: 0,
                                    column = colMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                                    severity = severity,
                                    message = messageMatch.groupValues[1],
                                    rule = codeMatch?.groupValues?.get(1) ?: "SC",
                                    filePath = filePath
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback - return empty list
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install ShellCheck: brew install shellcheck or apt-get install shellcheck"
}