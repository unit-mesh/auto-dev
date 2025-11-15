package cc.unitmesh.linter

/**
 * Represents a single linting issue found in code
 */
data class LintIssue(
    val line: Int,
    val column: Int = 0,
    val severity: LintSeverity,
    val message: String,
    val rule: String? = null,
    val suggestion: String? = null,
    val filePath: String? = null
)

/**
 * Severity levels for lint issues
 */
enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * Result of running a linter on a file or project
 */
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
 * Summary of linters available for files
 */
data class LinterSummary(
    val totalLinters: Int,
    val availableLinters: List<LinterAvailability>,
    val unavailableLinters: List<LinterAvailability>,
    val fileMapping: Map<String, List<String>> // file path -> linter names
)

/**
 * Linter availability information
 */
data class LinterAvailability(
    val name: String,
    val isAvailable: Boolean,
    val version: String? = null,
    val supportedFiles: List<String> = emptyList(),
    val installationInstructions: String? = null
)

