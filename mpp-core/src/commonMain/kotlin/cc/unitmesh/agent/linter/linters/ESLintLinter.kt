package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class ESLintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "eslint"
    override val description = "JavaScript and TypeScript linter"
    override val supportedExtensions = listOf("js", "jsx", "ts", "tsx", "mjs", "cjs")

    override fun getVersionCommand() = "eslint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "eslint --no-config-lookup --rule 'no-var:error,no-unused-vars:warn,semi:error,eqeqeq:error,no-console:warn' \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseESLintOutput(output, filePath)

    companion object {
        /**
         * Parse eslint output format
         * Example:
         * /path/to/file.js
         *    3:1   error    Unexpected var, use let or const instead     no-var
         *    4:15  error    Missing semicolon                            semi
         */
        fun parseESLintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // eslint format: line:column  level  message  rule
            val pattern = Regex("""^\s*(\d+):(\d+)\s+(error|warning)\s+(.+?)\s{2,}(\S+)\s*$""")

            for (line in output.lines()) {
                val match = pattern.find(line)
                if (match != null) {
                    val (lineNum, col, level, message, rule) = match.destructured

                    val severity = when (level.lowercase()) {
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
        "Install ESLint: npm install -g eslint"
}

