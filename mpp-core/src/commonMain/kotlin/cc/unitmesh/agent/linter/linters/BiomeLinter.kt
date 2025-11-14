package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

/**
 * ESLint/Biome linter for JavaScript/TypeScript
 */
class BiomeLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "biome"
    override val description = "Fast formatter and linter for JavaScript, TypeScript, JSON, and CSS"
    override val supportedExtensions = listOf("js", "jsx", "ts", "tsx", "json", "css")

    override fun getVersionCommand() = "biome --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "biome check --reporter=json \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseBiomeOutput(output, filePath)

    companion object {
        /**
         * Parse Biome JSON output
         * Biome outputs a JSON format with diagnostics array
         */
        fun parseBiomeOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            try {
                // Biome JSON parsing - simplified approach
                if (output.contains("\"diagnostics\"")) {
                    // Look for diagnostic objects
                    val diagnosticPattern = Regex("""\{[^}]*"severity"[^}]*\}""")
                    val matches = diagnosticPattern.findAll(output)

                    for (match in matches) {
                        val json = match.value
                        
                        // Extract fields
                        val severityMatch = Regex(""""severity"\s*:\s*"([^"]+)"""").find(json)
                        val messageMatch = Regex(""""message"\s*:\s*"([^"]+)"""").find(json)
                        
                        if (messageMatch != null) {
                            val severity = when (severityMatch?.groupValues?.get(1)?.lowercase()) {
                                "error" -> LintSeverity.ERROR
                                "warning" -> LintSeverity.WARNING
                                else -> LintSeverity.INFO
                            }

                            issues.add(
                                LintIssue(
                                    line = 0, // Biome's JSON structure is complex, simplified here
                                    column = 0,
                                    severity = severity,
                                    message = messageMatch.groupValues[1],
                                    rule = null,
                                    filePath = filePath
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to simple parsing
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install Biome: npm install -g @biomejs/biome or pnpm add -g @biomejs/biome"
}