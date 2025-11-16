package cc.unitmesh.agent.linter

import kotlinx.serialization.Serializable


@Serializable
data class LintFileResult(
    val filePath: String,
    val linterName: String,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issues: List<LintIssue>
)

/**
 * UI-friendly lint severity
 */
@Serializable
enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

@Serializable
data class LintIssue(
    val line: Int,
    val column: Int = 0,
    val severity: LintSeverity,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null,
    val filePath: String? = null
)

@Serializable
data class LintResult(
    val filePath: String,
    val issues: List<LintIssue>,
    val success: Boolean,
    val errorMessage: String? = null,
    val linterName: String
) {
    val hasIssues: Boolean get() = issues.isNotEmpty()
    val errorCount: Int get() = issues.count { it.severity == LintSeverity.ERROR }
    val warningCount: Int get() = issues.count { it.severity == LintSeverity.WARNING }
}

/**
 * Base interface for all linters
 */
interface Linter {
    /**
     * Name of the linter (e.g., "eslint", "detekt", "ruff")
     */
    val name: String
    
    /**
     * Description of what this linter checks
     */
    val description: String
    
    /**
     * Supported file extensions (e.g., ["kt", "kts"] for Kotlin)
     */
    val supportedExtensions: List<String>
    
    /**
     * Check if this linter is available in the system
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Lint a single file
     */
    suspend fun lintFile(filePath: String, projectPath: String): LintResult
    
    /**
     * Lint multiple files
     */
    suspend fun lintFiles(filePaths: List<String>, projectPath: String): List<LintResult> {
        return filePaths.map { lintFile(it, projectPath) }
    }
    
    /**
     * Get installation instructions if linter is not available
     */
    fun getInstallationInstructions(): String
}

/**
 * Summary of lint issues found in files
 * Focused on what matters: which files have issues, what severity, and what the issues are
 */
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
        fun format(linterSummary: cc.unitmesh.agent.linter.LinterSummary): String {
            return buildString {
                appendLine("## Lint Results Summary")
                appendLine("Files analyzed: ${linterSummary.totalFiles} | Files with issues: ${linterSummary.filesWithIssues}")
                appendLine("Total issues: ${linterSummary.totalIssues} (❌ ${linterSummary.errorCount} errors, ⚠️ ${linterSummary.warningCount} warnings, ℹ️ ${linterSummary.infoCount} info)")
                
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
                            file.topIssues.forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
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
                            file.topIssues.take(3).forEach { issue ->
                                appendLine("  - Line ${issue.line}: ${issue.message} [${issue.rule ?: "unknown"}]")
                            }
                            if (file.hasMoreIssues) {
                                appendLine("  - ... and ${file.totalIssues - file.topIssues.size} more issues")
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

/**
 * Per-file lint issue summary
 */
data class FileLintSummary(
    val filePath: String,
    val linterName: String,
    val totalIssues: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val topIssues: List<LintIssue>, // Top 5 most important issues
    val hasMoreIssues: Boolean
)

/**
 * Registry for managing available linters
 */
class LinterRegistry {
    private val linters = mutableMapOf<String, Linter>()

    /**
     * Register a linter
     */
    fun register(linter: Linter) {
        linters[linter.name] = linter
    }

    /**
     * Get linter by name
     */
    fun getLinter(name: String): Linter? {
        return linters[name]
    }

    /**
     * Get all registered linters
     */
    fun getAllLinters(): List<Linter> {
        return linters.values.toList()
    }

    /**
     * Find suitable linters for a file based on extension
     */
    fun findLintersForFile(filePath: String): List<Linter> {
        val extension = filePath.substringAfterLast('.', "")
        return linters.values.filter { linter ->
            linter.supportedExtensions.any { it.equals(extension, ignoreCase = true) }
        }
    }

    /**
     * Find suitable linters for multiple files
     */
    fun findLintersForFiles(filePaths: List<String>): List<Linter> {
        val extensions = filePaths.map { it.substringAfterLast('.', "") }.toSet()
        return linters.values.filter { linter ->
            linter.supportedExtensions.any { ext ->
                extensions.any { it.equals(ext, ignoreCase = true) }
            }
        }.distinctBy { it.name }
    }

    /**
     * Get summary of linters for specific files
     * Actually runs linters and collects real issues
     */
    suspend fun getLinterSummaryForFiles(filePaths: List<String>, projectPath: String = "."): LinterSummary {
        if (filePaths.isEmpty()) {
            return LinterSummary(
                totalFiles = 0,
                filesWithIssues = 0,
                totalIssues = 0,
                errorCount = 0,
                warningCount = 0,
                infoCount = 0,
                fileIssues = emptyList(),
                executedLinters = emptyList()
            )
        }

        val suitableLinters = findLintersForFiles(filePaths)
        val availableLinters = suitableLinters.filter { it.isAvailable() }
        
        if (availableLinters.isEmpty()) {
            // No linters available, return empty summary
            return LinterSummary(
                totalFiles = filePaths.size,
                filesWithIssues = 0,
                totalIssues = 0,
                errorCount = 0,
                warningCount = 0,
                infoCount = 0,
                fileIssues = emptyList(),
                executedLinters = emptyList()
            )
        }

        val fileIssues = mutableListOf<FileLintSummary>()
        val executedLinters = mutableSetOf<String>()
        
        var totalIssues = 0
        var totalErrors = 0
        var totalWarnings = 0
        var totalInfo = 0

        // Run each available linter on its supported files
        for (linter in availableLinters) {
            try {
                val supportedFiles = filePaths.filter { path ->
                    val ext = path.substringAfterLast('.', "").lowercase()
                    linter.supportedExtensions.any { it.equals(ext, ignoreCase = true) }
                }

                if (supportedFiles.isEmpty()) continue

                // Lint the supported files
                val results = linter.lintFiles(supportedFiles, projectPath)
                executedLinters.add(linter.name)

                // Process results
                for (result in results) {
                    if (result.issues.isEmpty()) continue

                    val issues = result.issues
                    val errorCount = issues.count { it.severity == LintSeverity.ERROR }
                    val warningCount = issues.count { it.severity == LintSeverity.WARNING }
                    val infoCount = issues.count { it.severity == LintSeverity.INFO }

                    // Take top 5 issues, prioritizing errors > warnings > info
                    val topIssues = issues
                        .sortedWith(compareBy<LintIssue> {
                            when (it.severity) {
                                LintSeverity.ERROR -> 0
                                LintSeverity.WARNING -> 1
                                LintSeverity.INFO -> 2
                            }
                        }.thenBy { it.line })
                        .take(5)

                    fileIssues.add(
                        FileLintSummary(
                            filePath = result.filePath,
                            linterName = linter.name,
                            totalIssues = issues.size,
                            errorCount = errorCount,
                            warningCount = warningCount,
                            infoCount = infoCount,
                            topIssues = topIssues,
                            hasMoreIssues = issues.size > 5
                        )
                    )

                    totalIssues += issues.size
                    totalErrors += errorCount
                    totalWarnings += warningCount
                    totalInfo += infoCount
                }
            } catch (e: Exception) {
                // Log error but continue with other linters
                println("Warning: Linter ${linter.name} failed: ${e.message}")
            }
        }

        return LinterSummary(
            totalFiles = filePaths.size,
            filesWithIssues = fileIssues.size,
            totalIssues = totalIssues,
            errorCount = totalErrors,
            warningCount = totalWarnings,
            infoCount = totalInfo,
            fileIssues = fileIssues.sortedByDescending { it.errorCount },
            executedLinters = executedLinters.toList()
        )
    }

    companion object {
        private var instance: LinterRegistry? = null

        fun getInstance(): LinterRegistry {
            if (instance == null) {
                instance = LinterRegistry()
                // Register default linters
                registerPlatformLinters(instance!!)
            }
            return instance!!
        }
    }
}

/**
 * Platform-specific linter registration
 * This function is implemented in each platform's source set
 */
expect fun registerPlatformLinters(registry: LinterRegistry)

