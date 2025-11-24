package cc.unitmesh.devins.ui.compose.agent.codereview.analysis

import cc.unitmesh.agent.linter.LintFileResult
import cc.unitmesh.agent.linter.LintIssue
import cc.unitmesh.agent.linter.LintSeverity
import cc.unitmesh.agent.logging.AutoDevLogger
import cc.unitmesh.devins.ui.compose.agent.codereview.ModifiedCodeRange

/**
 * Executes linters and processes lint results.
 * This is a pure non-AI component that runs static analysis tools and filters results.
 */
class LintExecutor {
    /**
     * Run linters on the specified files and collect results.
     *
     * @param filePaths List of file paths to lint
     * @param projectPath Root path of the project
     * @param modifiedCodeRanges Optional map of modified code ranges for filtering
     * @param progressCallback Optional callback for progress updates
     * @return List of file lint results
     */
    suspend fun runLint(
        filePaths: List<String>,
        projectPath: String,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>> = emptyMap(),
        progressCallback: ((String) -> Unit)? = null
    ): List<LintFileResult> {
        val allFileLintResults = mutableListOf<LintFileResult>()

        try {
            val linterRegistry = cc.unitmesh.agent.linter.LinterRegistry.getInstance()
            val linters = linterRegistry.findLintersForFiles(filePaths)

            if (linters.isEmpty()) {
                progressCallback?.invoke("No suitable linters found for the given files.\n")
                return emptyList()
            }

            progressCallback?.invoke("Running linters: ${linters.joinToString(", ") { it.name }}\n")

            if (modifiedCodeRanges.isNotEmpty()) {
                val totalRanges = modifiedCodeRanges.values.sumOf { it.size }
                progressCallback?.invoke("   Filtering results to $totalRanges modified code element(s)\n")
            }
            progressCallback?.invoke("\n")

            // Run each linter
            for (linter in linters) {
                if (!linter.isAvailable()) {
                    progressCallback?.invoke("${linter.name} is not installed\n")
                    progressCallback?.invoke("${linter.getInstallationInstructions()}\n\n")
                    continue
                }

                progressCallback?.invoke("Running ${linter.name}...\n")

                // Lint files
                val results = linter.lintFiles(filePaths, projectPath)

                // Convert to UI model and aggregate
                for (result in results) {
                    if (result.hasIssues) {
                        val filteredIssues = filterIssuesByModifiedRanges(
                            result.issues,
                            result.filePath,
                            modifiedCodeRanges
                        )

                        if (filteredIssues.isEmpty()) continue

                        val uiIssues = filteredIssues.map { issue ->
                            LintIssue(
                                line = issue.line,
                                column = issue.column,
                                severity = when (issue.severity) {
                                    LintSeverity.ERROR -> LintSeverity.ERROR
                                    LintSeverity.WARNING -> LintSeverity.WARNING
                                    LintSeverity.INFO -> LintSeverity.INFO
                                },
                                message = issue.message,
                                rule = issue.rule,
                                suggestion = issue.suggestion
                            )
                        }

                        val errorCount = filteredIssues.count { it.severity == LintSeverity.ERROR }
                        val warningCount =
                            filteredIssues.count { it.severity == LintSeverity.WARNING }
                        val infoCount = filteredIssues.count { it.severity == LintSeverity.INFO }

                        allFileLintResults.add(
                            LintFileResult(
                                filePath = result.filePath,
                                linterName = result.linterName,
                                errorCount = errorCount,
                                warningCount = warningCount,
                                infoCount = infoCount,
                                issues = uiIssues
                            )
                        )

                        progressCallback?.invoke("${result.filePath}\n")

                        if (modifiedCodeRanges.isNotEmpty()) {
                            val totalIssues = result.issues.size
                            val filteredCount = filteredIssues.size
                            progressCallback?.invoke("     Found: $filteredCount/$totalIssues issues in modified code\n")
                        }

                        progressCallback?.invoke("     Errors: $errorCount, Warnings: $warningCount\n")

                        // Show first few issues
                        filteredIssues.take(5).forEach { issue ->
                            val severityIcon = when (issue.severity) {
                                LintSeverity.ERROR -> "❌"
                                LintSeverity.WARNING -> "⚠️"
                                LintSeverity.INFO -> "ℹ️"
                            }
                            progressCallback?.invoke("     $severityIcon Line ${issue.line}: ${issue.message}\n")
                        }

                        if (filteredIssues.size > 5) {
                            progressCallback?.invoke("     ... and ${filteredIssues.size - 5} more issues\n")
                        }
                        progressCallback?.invoke("\n")
                    }
                }
            }

            progressCallback?.invoke("Linting complete\n")

        } catch (e: Exception) {
            AutoDevLogger.error("LintExecutor") { "Failed to run lint: ${e.message}" }
            progressCallback?.invoke("Error running linters: ${e.message}\n")
        }

        return allFileLintResults
    }

    /**
     * Filter lint issues to only those in modified code ranges.
     *
     * @param issues All lint issues for a file
     * @param filePath The file path
     * @param modifiedCodeRanges Map of file paths to modified code ranges
     * @return Filtered list of issues
     */
    fun filterIssuesByModifiedRanges(
        issues: List<LintIssue>,
        filePath: String,
        modifiedCodeRanges: Map<String, List<ModifiedCodeRange>>
    ): List<LintIssue> {
        if (modifiedCodeRanges.isEmpty()) {
            return issues
        }

        val ranges = modifiedCodeRanges[filePath] ?: emptyList()
        if (ranges.isEmpty()) {
            return emptyList()
        }

        return issues.filter { issue ->
            ranges.any { range ->
                issue.line >= range.startLine && issue.line <= range.endLine
            }
        }
    }
}
