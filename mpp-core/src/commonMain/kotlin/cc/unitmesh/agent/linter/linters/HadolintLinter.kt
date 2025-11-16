package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class HadolintLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "hadolint"
    override val description = "Dockerfile linter"
    override val supportedExtensions = listOf("dockerfile")

    override fun getVersionCommand() = "hadolint --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "hadolint \"$filePath\""

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseHadolintOutput(output, filePath)

    companion object {
        /**
         * Parse hadolint output format
         * Example: Dockerfile.bad:3 DL3006 warning: Always tag the version of an image explicitly
         */
        fun parseHadolintOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()

            // hadolint format: filename:line code level: message
            val pattern = Regex("""^(.+?):(\d+)\s+(\S+)\s+(error|warning|info|style):\s+(.+)$""")

            for (line in output.lines()) {
                val match = pattern.find(line.trim())
                if (match != null) {
                    val (_, lineNum, code, level, message) = match.destructured

                    val severity = when (level.lowercase()) {
                        "error" ->cc.unitmesh.agent.linter.LintSeverity.ERROR
                        "warning" ->cc.unitmesh.agent.linter.LintSeverity.WARNING
                        else ->cc.unitmesh.agent.linter.LintSeverity.INFO
                    }

                    issues.add(
                        LintIssue(
                            line = lineNum.toIntOrNull() ?: 0,
                            column = 1, // hadolint doesn't provide column info
                            severity = severity,
                            message = message.trim(),
                            rule = code,
                            filePath = filePath
                        )
                    )
                }
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install Hadolint: https://github.com/hadolint/hadolint#install"
}

