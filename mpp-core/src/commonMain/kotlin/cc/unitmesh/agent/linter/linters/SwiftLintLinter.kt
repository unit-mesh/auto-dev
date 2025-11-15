package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class SwiftLintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "swiftlint"
    override val description = "Swift code linter and formatter"
    override val supportedExtensions = listOf("swift")

    override fun getVersionCommand() = "swiftlint version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "swiftlint lint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseSwiftLintOutput(output, filePath)

    companion object {
        /**
         * Parse swiftlint output format
         * Example: /path/to/file.swift:26:23: error: Force Cast Violation: Force casts should be avoided (force_cast)
         */
        fun parseSwiftLintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()
            
            // Pattern: filename:line:column: severity: message (rule)
            val pattern = Regex("""^(.+?):(\d+):(\d+):\s*(error|warning):\s*(.+?)\s*\(([^)]+)\)\s*$""")
            
            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, col, severityStr, message, rule) = match.destructured
                    
                    val severity = when (severityStr.lowercase()) {
                        "error" -> LintSeverity.ERROR
                        "warning" -> LintSeverity.WARNING
                        else -> LintSeverity.INFO
                    }
                    
                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = col.toIntOrNull() ?: 0,
                            severity = severity,
                            message = message.trim(),
                            rule = rule,
                            filePath = filePath
                        )
                    )
                }
            }
            
            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install SwiftLint: brew install swiftlint (macOS) or see https://github.com/realm/SwiftLint"
}

