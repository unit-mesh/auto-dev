package cc.unitmesh.agent.linter

import kotlinx.serialization.Serializable

/**
 * Summary of lint issues found in files
 * Focused on what matters: which files have issues, what severity, and what the issues are
 */
@Serializable
data class LinterSummary(
    val totalFiles: Int,
    val filesWithIssues: Int,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val fileIssues: List<FileLintSummary>, // Per-file issue breakdown
    val executedLinters: List<String> // Which linters actually ran
) {
    companion object {
        fun format(linterSummary: LinterSummary): String {
            return buildString {
                appendLine("## Lint Results Summary")

                if (linterSummary.executedLinters.isNotEmpty()) {
                    appendLine("Linters executed: ${linterSummary.executedLinters.joinToString(", ")}")
                }
                appendLine()

                if (linterSummary.fileIssues.isNotEmpty()) {
                    // Group by severity priority: errors first, then warnings, then info
                    val filesWithErrors = linterSummary.fileIssues.filter { it.errorCount > 0 }
                    val filesWithWarnings = linterSummary.fileIssues.filter { it.errorCount == 0 && it.warningCount > 0 }
                    val filesWithInfo = linterSummary.fileIssues.filter { it.errorCount == 0 && it.warningCount == 0 && it.infoCount > 0 }

                    if (filesWithErrors.isNotEmpty()) {
                        appendLine("### ❌ Files with Errors (${filesWithErrors.size})")
                        filesWithErrors.forEach { file ->
                            appendLine("**${file.filePath}** (${file.errorCount} errors, ${file.warningCount} warnings)")
                            // Show all topIssues (up to 10), with severity indicators
                            file.topIssues.forEach { issue ->
                                val icon = when (issue.severity) {
                                    LintSeverity.ERROR -> "❌"
                                    LintSeverity.WARNING -> "⚠️"
                                    LintSeverity.INFO -> "ℹ️"
                                }
                                appendLine("  - $icon Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
                            }
                            if (file.hasMoreIssues) {
                                appendLine("  - ... and ${file.totalIssues - file.topIssues.size} more issues")
                            }
                        }
                        appendLine()
                    }

                    if (filesWithWarnings.isNotEmpty()) {
                        appendLine("### ⚠️ Files with Warnings (${filesWithWarnings.size})")
                        filesWithWarnings.forEach { file ->
                            appendLine("**${file.filePath}** (${file.warningCount} warnings)")
                            // Show first 5 issues for warning-only files to keep output manageable
                            file.topIssues.take(5).forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
                            }
                            if (file.hasMoreIssues || file.topIssues.size > 5) {
                                val remaining = if (file.hasMoreIssues) {
                                    file.totalIssues - 5
                                } else {
                                    file.topIssues.size - 5
                                }
                                appendLine("  - ... and $remaining more issues")
                            }
                        }
                        appendLine()
                    }

                    if (filesWithInfo.isNotEmpty() && filesWithInfo.size <= 5) {
                        appendLine("### ℹ️ Files with Info (${filesWithInfo.size})")
                        filesWithInfo.forEach { file ->
                            appendLine("**${file.filePath}** (${file.infoCount} info)")
                        }
                    }
                } else {
                    appendLine("✅ No issues found!")
                }
            }
        }
    }
}