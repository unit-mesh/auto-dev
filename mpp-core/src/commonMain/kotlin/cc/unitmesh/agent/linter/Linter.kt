package cc.unitmesh.agent.linter

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
     */
    suspend fun getLinterSummaryForFiles(filePaths: List<String>): LinterSummary {
        val suitableLinters = findLintersForFiles(filePaths)

        val availabilities = mutableListOf<LinterAvailability>()
        val fileMapping = mutableMapOf<String, MutableList<String>>()

        for (linter in suitableLinters) {
            val isAvailable = linter.isAvailable()
            val supportedFiles = filePaths.filter { path ->
                val ext = path.substringAfterLast('.', "").lowercase()
                linter.supportedExtensions.any { it.equals(ext, ignoreCase = true) }
            }

            val availability = LinterAvailability(
                name = linter.name,
                isAvailable = isAvailable,
                supportedFiles = supportedFiles,
                installationInstructions = if (!isAvailable) linter.getInstallationInstructions() else null
            )
            availabilities.add(availability)

            // Build file mapping
            for (file in supportedFiles) {
                fileMapping.getOrPut(file) { mutableListOf() }.add(linter.name)
            }
        }

        val available = availabilities.filter { it.isAvailable }
        val unavailable = availabilities.filter { !it.isAvailable }

        return LinterSummary(
            totalLinters = availabilities.size,
            availableLinters = available,
            unavailableLinters = unavailable,
            fileMapping = fileMapping
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

/**
 * Language detection utility
 */
object LanguageDetector {
    fun detectLanguage(filePath: String): String? {
        val extension = filePath.substringAfterLast('.', "")
        return when (extension.lowercase()) {
            "kt", "kts" -> "Kotlin"
            "java" -> "Java"
            "js", "jsx" -> "JavaScript"
            "ts", "tsx" -> "TypeScript"
            "py" -> "Python"
            "rs" -> "Rust"
            "go" -> "Go"
            "swift" -> "Swift"
            "c", "h" -> "C"
            "cpp", "cc", "cxx", "hpp" -> "C++"
            "cs" -> "C#"
            "rb" -> "Ruby"
            "php" -> "PHP"
            "html", "htm" -> "HTML"
            "css", "scss", "sass" -> "CSS"
            "json" -> "JSON"
            "xml" -> "XML"
            "yaml", "yml" -> "YAML"
            "md" -> "Markdown"
            "sh", "bash" -> "Shell"
            "sql" -> "SQL"
            else -> null
        }
    }
    
    fun getLinterNamesForLanguage(language: String): List<String> {
        return when (language) {
            "Kotlin" -> listOf("detekt")
            "Java" -> listOf("pmd")
            "JavaScript", "TypeScript" -> listOf("biome", "oxlint")
            "Python" -> listOf("ruff", "pylint", "flake8")
            "Rust" -> listOf("clippy")
            "Go" -> listOf("golangci-lint")
            "Ruby" -> listOf("rubocop", "brakeman")
            "PHP" -> listOf("phpstan", "phpmd", "phpcs")
            "Shell" -> listOf("shellcheck")
            "Markdown" -> listOf("markdownlint")
            "YAML" -> listOf("yamllint")
            "Docker" -> listOf("hadolint")
            "SQL" -> listOf("sqlfluff")
            "Swift" -> listOf("swiftlint")
            "HTML" -> listOf("htmlhint")
            "CSS" -> listOf("biome")
            else -> emptyList()
        }
    }
}

