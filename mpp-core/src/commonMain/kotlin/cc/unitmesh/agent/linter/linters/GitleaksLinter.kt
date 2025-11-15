package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class GitleaksLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "gitleaks"
    override val description = "Secret scanner to detect hardcoded secrets"
    override val supportedExtensions = listOf("*") // Supports all file types

    override fun getVersionCommand() = "gitleaks version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "gitleaks detect --no-git -f \"$filePath\" -v"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseGitleaksOutput(output, filePath)

    companion object {
        /**
         * Parse gitleaks verbose output format
         * Example:
         * Finding:     API_KEY = "sk-1234567890abcdef"
         * Secret:      sk-1234567890abcdef
         * RuleID:      generic-api-key
         * Entropy:     4.247928
         * File:        config.py
         * Line:        23
         * Fingerprint: config.py:generic-api-key:23
         */
        fun parseGitleaksOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()
            
            // Split by "Finding:" to separate each leak
            val findings = output.split("Finding:")
            
            for (finding in findings) {
                if (finding.trim().isEmpty() || !finding.contains("RuleID:")) continue
                
                var ruleId = ""
                var line = 0
                var message = ""
                
                // Extract information
                for (findingLine in finding.lines()) {
                    val trimmed = findingLine.trim()
                    when {
                        trimmed.startsWith("RuleID:") -> {
                            ruleId = trimmed.substringAfter("RuleID:").trim()
                        }
                        trimmed.startsWith("Line:") -> {
                            line = trimmed.substringAfter("Line:").trim().toIntOrNull() ?: 0
                        }
                        trimmed.startsWith("Secret:") -> {
                            // Extract secret (might contain ANSI codes)
                            val secret = trimmed.substringAfter("Secret:").trim()
                            message = "Secret detected: $ruleId"
                        }
                    }
                }
                
                if (ruleId.isNotEmpty() && line > 0) {
                    issues.add(
                        LintIssue(
                            line = line,
                            column = 1, // Gitleaks doesn't provide column
                            severity = LintSeverity.ERROR, // All secrets are errors
                            message = message.ifEmpty { "Potential secret detected" },
                            rule = ruleId,
                            filePath = filePath
                        )
                    )
                }
            }
            
            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install Gitleaks: brew install gitleaks (macOS) or see https://github.com/gitleaks/gitleaks"
}

