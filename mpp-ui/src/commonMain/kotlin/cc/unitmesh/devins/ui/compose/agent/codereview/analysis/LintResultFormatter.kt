package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.devins.ui.compose.agent.codereview.FileLintResult

/**
 * Formats lint results into human-readable strings.
 * This is a pure non-AI utility for converting structured lint data to text.
 */
class LintResultFormatter {
    /**
     * Format lint results for display or analysis prompts.
     *
     * @param lintResults List of lint results per file
     * @return Map of file paths to formatted lint result strings
     */
    fun formatLintResults(lintResults: List<FileLintResult>): Map<String, String> {
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
    fun formatSummary(lintResults: List<FileLintResult>): String {
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
