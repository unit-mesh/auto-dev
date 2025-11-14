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

    override fun parseOutput(output: String, filePath: String): List<LintIssue> {
        // Parse Biome JSON output
        // This is a simplified version - real implementation would use JSON parsing
        val issues = mutableListOf<LintIssue>()

        try {
            // Biome outputs JSON format, we'll parse it simply here
            // In real implementation, use kotlinx.serialization
            val lines = output.lines()
            for (line in lines) {
                if (line.contains("\"severity\"")) {
                    // Extract basic info from JSON line
                    val severity = when {
                        line.contains("error") -> LintSeverity.ERROR
                        line.contains("warning") -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    }

                    issues.add(
                        LintIssue(
                            line = 0, // Would extract from JSON
                            column = 0,
                            severity = severity,
                            message = "Biome issue found", // Would extract from JSON
                            rule = null,
                            filePath = filePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to simple parsing
        }

        return issues
    }

    override fun getInstallationInstructions() =
        "Install Biome: npm install -g @biomejs/biome or pnpm add -g @biomejs/biome"
}