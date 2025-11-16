package cc.unitmesh.agent.linter.linters

import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.ShellBasedLinter
import cc.unitmesh.agent.tool.shell.ShellExecutor

class CheckovLinter(shellExecutor: ShellExecutor) : ShellBasedLinter(shellExecutor) {
    override val name = "checkov"
    override val description = "Infrastructure as Code (IaC) static analysis tool"
    override val supportedExtensions = listOf("tf", "yaml", "yml", "json", "dockerfile")

    override fun getVersionCommand() = "checkov --version"

    override fun getLintCommand(filePath: String, projectPath: String) =
        "checkov -f \"$filePath\" --compact --skip-download"

    override fun parseOutput(output: String, filePath: String): List<LintIssue> =
        Companion.parseCheckovOutput(output, filePath)

    companion object {
        /**
         * Parse checkov output format
         * Example:
         * Check: CKV_AWS_260: "Ensure no security groups allow ingress from 0.0.0.0:0 to port 80"
         *     FAILED for resource: aws_security_group.example
         *     File: /bad_terraform.tf:8-19
         */
        fun parseCheckovOutput(output: String, filePath: String): List<LintIssue> {
            val issues = mutableListOf<LintIssue>()
            val lines = output.lines()

            var i = 0
            while (i < lines.size) {
                val line = lines[i].trim()

                // Parse check line: Check: CODE: "message"
                if (line.startsWith("Check:")) {
                    val checkMatch = Regex("""Check:\s+(\S+):\s+"([^"]+)".*""").find(line)
                    if (checkMatch != null) {
                        val (code, message) = checkMatch.destructured

                        // Look for FAILED/PASSED in next lines
                        var status = ""
                        var fileInfo = ""

                        for (j in i + 1 until minOf(i + 4, lines.size)) {
                            val nextLine = lines[j].trim()

                            if (nextLine.contains("FAILED") || nextLine.contains("PASSED")) {
                                status = nextLine
                            } else if (nextLine.startsWith("File:")) {
                                fileInfo = nextLine
                                break
                            }
                        }

                        // Only report FAILED checks
                        if (status.contains("FAILED")) {
                            val fileMatch = Regex("""File:\s+(.+):(\d+)-(\d+)""").find(fileInfo)
                            if (fileMatch != null) {
                                val (_, startLine, endLine) = fileMatch.destructured

                                issues.add(
                                    LintIssue(
                                        line = startLine.toIntOrNull() ?: 0,
                                        column = 1, // Checkov doesn't provide column info
                                        severity =cc.unitmesh.agent.linter.LintSeverity.WARNING,
                                        message = message.trim(),
                                        rule = code,
                                        filePath = filePath
                                    )
                                )
                            }
                        }
                    }
                }
                i++
            }

            return issues
        }
    }

    override fun getInstallationInstructions() =
        "Install Checkov: pip install checkov"
}

