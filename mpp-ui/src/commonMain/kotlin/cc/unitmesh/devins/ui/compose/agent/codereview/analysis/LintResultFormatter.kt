package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.agent.linter.LintFileResult

class LintResultFormatter {
    fun formatLintResults(lintResults: List<LintFileResult>): Map<String, String> {
        val lintResultsMap = mutableMapOf<String, String>()

        lintResults.forEach { fileResult ->
            val formatted = buildString {
                val totalCount = fileResult.errorCount + fileResult.warningCount + fileResult.infoCount
                appendLine("File: ${fileResult.filePath}")
                appendLine("Total Issues: $totalCount")
                appendLine("  Errors: ${fileResult.errorCount}")
                appendLine("  Warnings: ${fileResult.warningCount}")
                appendLine("  Info: ${fileResult.infoCount}")
                appendLine()

                if (fileResult.issues.isNotEmpty()) {
                    appendLine("Issues:")
                    fileResult.issues.forEach { issue ->
                        appendLine("  [${issue.severity}] Line ${issue.line}: ${issue.message}")
                        if (issue.rule?.isNotBlank() == true) {
                            appendLine("    Rule: ${issue.rule}")
                        }
                    }
                }
            }
            lintResultsMap[fileResult.filePath] = formatted
        }

        return lintResultsMap
    }

    /**
     * Create a summary of lint results across all files.
     *
     * @param lintResults List of lint results per file
     * @return Summary string with total counts
     */
    fun formatSummary(lintResults: List<LintFileResult>): String {
        val totalErrors = lintResults.sumOf { it.errorCount }
        val totalWarnings = lintResults.sumOf { it.warningCount }
        val totalInfo = lintResults.sumOf { it.infoCount }
        val totalFiles = lintResults.size

        return buildString {
            appendLine("Lint Summary:")
            appendLine("  Files analyzed: $totalFiles")
            appendLine("  Total errors: $totalErrors")
            appendLine("  Total warnings: $totalWarnings")
            appendLine("  Total info: $totalInfo")
        }
    }
}
