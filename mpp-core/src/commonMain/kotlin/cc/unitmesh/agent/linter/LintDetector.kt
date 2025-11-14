package cc.unitmesh.agent.linter

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig

/**
 * Configuration for a linter from linter.md
 */
data class LinterConfig(
    val name: String,
    val technology: String,
    val category: String,
    val fileExtensions: List<String>,
    val versionCommand: String,
    val description: String = ""
)

/**
 * Result of linter detection
 */
data class LinterDetectionResult(
    val linterName: String,
    val isAvailable: Boolean,
    val version: String? = null,
    val supportedFiles: List<String> = emptyList(),
    val installationInstructions: String? = null
)

/**
 * Detects available linters in the system
 * 
 * This class scans the system to find which linters are installed and available,
 * based on the configuration from linter.md
 */
class LintDetector(
    private val shellExecutor: ShellExecutor
) {
    private val logger = getLogger("LintDetector")
    
    companion object {
        /**
         * Linter configurations based on linter.md
         * Maps linter name to its configuration
         */
        private val LINTER_CONFIGS = mapOf(
            // JavaScript/TypeScript
            "biome" to LinterConfig(
                name = "biome",
                technology = "JavaScript/TypeScript/JSON/CSS",
                category = "Code Quality",
                fileExtensions = listOf("js", "jsx", "ts", "tsx", "json", "css"),
                versionCommand = "biome --version",
                description = "Fast formatter and linter for JavaScript, TypeScript, JSON, and CSS"
            ),
            "oxlint" to LinterConfig(
                name = "oxlint",
                technology = "JavaScript/TypeScript",
                category = "Code Quality",
                fileExtensions = listOf("js", "jsx", "ts", "tsx"),
                versionCommand = "oxlint --version",
                description = "Fast JavaScript/TypeScript linter"
            ),
            
            // Kotlin
            "detekt" to LinterConfig(
                name = "detekt",
                technology = "Kotlin",
                category = "Code Quality",
                fileExtensions = listOf("kt", "kts"),
                versionCommand = "detekt --version",
                description = "Static code analysis for Kotlin"
            ),
            
            // Python
            "ruff" to LinterConfig(
                name = "ruff",
                technology = "Python",
                category = "Code Quality",
                fileExtensions = listOf("py", "ipynb"),
                versionCommand = "ruff --version",
                description = "Fast Python linter"
            ),
            "pylint" to LinterConfig(
                name = "pylint",
                technology = "Python",
                category = "Code Quality",
                fileExtensions = listOf("py"),
                versionCommand = "pylint --version",
                description = "Python code static checker"
            ),
            "flake8" to LinterConfig(
                name = "flake8",
                technology = "Python",
                category = "Code Quality",
                fileExtensions = listOf("py"),
                versionCommand = "flake8 --version",
                description = "Python style guide enforcement"
            ),
            
            // Shell
            "shellcheck" to LinterConfig(
                name = "shellcheck",
                technology = "Shell",
                category = "Code Quality",
                fileExtensions = listOf("sh", "bash"),
                versionCommand = "shellcheck --version",
                description = "Static analysis tool for shell scripts"
            ),
            
            // Go
            "golangci-lint" to LinterConfig(
                name = "golangci-lint",
                technology = "Go",
                category = "Code Quality",
                fileExtensions = listOf("go"),
                versionCommand = "golangci-lint --version",
                description = "Fast linters runner for Go"
            ),
            
            // Rust
            "clippy" to LinterConfig(
                name = "clippy",
                technology = "Rust",
                category = "Code Quality",
                fileExtensions = listOf("rs"),
                versionCommand = "cargo clippy --version",
                description = "Rust linter"
            ),
            
            // Ruby
            "rubocop" to LinterConfig(
                name = "rubocop",
                technology = "Ruby",
                category = "Code Quality",
                fileExtensions = listOf("rb"),
                versionCommand = "rubocop --version",
                description = "Ruby static code analyzer"
            ),
            
            // PHP
            "phpstan" to LinterConfig(
                name = "phpstan",
                technology = "PHP",
                category = "Code Quality",
                fileExtensions = listOf("php"),
                versionCommand = "phpstan --version",
                description = "PHP static analysis tool"
            ),
            
            // Java
            "pmd" to LinterConfig(
                name = "pmd",
                technology = "Java",
                category = "Code Quality",
                fileExtensions = listOf("java"),
                versionCommand = "pmd --version",
                description = "Source code analyzer for Java"
            ),
            
            // Swift
            "swiftlint" to LinterConfig(
                name = "swiftlint",
                technology = "Swift",
                category = "Code Quality",
                fileExtensions = listOf("swift"),
                versionCommand = "swiftlint version",
                description = "Swift style and conventions tool"
            ),
            
            // Markdown
            "markdownlint" to LinterConfig(
                name = "markdownlint",
                technology = "Markdown",
                category = "Code Quality",
                fileExtensions = listOf("md"),
                versionCommand = "markdownlint --version",
                description = "Markdown linter"
            ),
            
            // YAML
            "yamllint" to LinterConfig(
                name = "yamllint",
                technology = "YAML",
                category = "Code Quality",
                fileExtensions = listOf("yaml", "yml"),
                versionCommand = "yamllint --version",
                description = "YAML linter"
            ),
            
            // Docker
            "hadolint" to LinterConfig(
                name = "hadolint",
                technology = "Docker",
                category = "Code Quality",
                fileExtensions = listOf("dockerfile"),
                versionCommand = "hadolint --version",
                description = "Dockerfile linter"
            ),
            
            // SQL
            "sqlfluff" to LinterConfig(
                name = "sqlfluff",
                technology = "SQL",
                category = "Code Quality",
                fileExtensions = listOf("sql"),
                versionCommand = "sqlfluff --version",
                description = "SQL linter"
            ),
            
            // HTML
            "htmlhint" to LinterConfig(
                name = "htmlhint",
                technology = "HTML",
                category = "Code Quality",
                fileExtensions = listOf("html", "htm"),
                versionCommand = "htmlhint --version",
                description = "HTML linter"
            ),
            
            // C/C++
            "cppcheck" to LinterConfig(
                name = "cppcheck",
                technology = "C/C++",
                category = "Code Quality",
                fileExtensions = listOf("c", "cpp", "cc", "cxx", "h", "hpp"),
                versionCommand = "cppcheck --version",
                description = "Static analysis tool for C/C++"
            )
        )
        
        /**
         * Get installation instructions for a linter
         */
        fun getInstallationInstructions(linterName: String): String {
            return when (linterName) {
                "biome" -> "npm install -g @biomejs/biome or pnpm add -g @biomejs/biome"
                "oxlint" -> "npm install -g oxlint"
                "detekt" -> "Add to build.gradle.kts or download CLI from https://github.com/detekt/detekt"
                "ruff" -> "pip install ruff or brew install ruff"
                "pylint" -> "pip install pylint"
                "flake8" -> "pip install flake8"
                "shellcheck" -> "brew install shellcheck or apt-get install shellcheck"
                "golangci-lint" -> "brew install golangci-lint or go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest"
                "clippy" -> "rustup component add clippy"
                "rubocop" -> "gem install rubocop"
                "phpstan" -> "composer require --dev phpstan/phpstan"
                "pmd" -> "brew install pmd or download from https://pmd.github.io/"
                "swiftlint" -> "brew install swiftlint"
                "markdownlint" -> "npm install -g markdownlint-cli"
                "yamllint" -> "pip install yamllint"
                "hadolint" -> "brew install hadolint"
                "sqlfluff" -> "pip install sqlfluff"
                "htmlhint" -> "npm install -g htmlhint"
                "cppcheck" -> "brew install cppcheck or apt-get install cppcheck"
                else -> "Please install $linterName manually"
            }
        }
    }
    
    /**
     * Detect all available linters in the system
     */
    suspend fun detectAvailableLinters(): List<LinterDetectionResult> {
        logger.info { "🔍 Detecting available linters..." }
        
        val results = mutableListOf<LinterDetectionResult>()
        
        for ((name, config) in LINTER_CONFIGS) {
            val result = checkLinterAvailability(config)
            results.add(result)
            
            if (result.isAvailable) {
                logger.info { "✅ Found $name ${result.version ?: ""}" }
            } else {
                logger.debug { "❌ $name not available" }
            }
        }
        
        return results
    }
    
    /**
     * Detect linters for specific files
     */
    suspend fun detectLintersForFiles(filePaths: List<String>): List<LinterDetectionResult> {
        logger.info { "🔍 Detecting linters for ${filePaths.size} files..." }
        
        // Get file extensions
        val extensions = filePaths.mapNotNull { path ->
            val ext = path.substringAfterLast('.', "")
            if (ext.isNotEmpty() && ext != path) ext.lowercase() else null
        }.toSet()
        
        logger.debug { "File extensions: $extensions" }
        
        // Find relevant linters
        val relevantLinters = LINTER_CONFIGS.values.filter { config ->
            config.fileExtensions.any { ext -> extensions.contains(ext.lowercase()) }
        }
        
        logger.info { "Found ${relevantLinters.size} relevant linters" }
        
        // Check availability
        val results = mutableListOf<LinterDetectionResult>()
        for (config in relevantLinters) {
            val supportedFiles = filePaths.filter { path ->
                val ext = path.substringAfterLast('.', "").lowercase()
                config.fileExtensions.any { it.equals(ext, ignoreCase = true) }
            }
            
            val result = checkLinterAvailability(config).copy(
                supportedFiles = supportedFiles
            )
            results.add(result)
        }
        
        return results
    }
    
    /**
     * Check if a specific linter is available
     */
    private suspend fun checkLinterAvailability(config: LinterConfig): LinterDetectionResult {
        return try {
            val shellConfig = ShellExecutionConfig(timeoutMs = 5000L)
            val result = shellExecutor.execute(config.versionCommand, shellConfig)
            
            if (result.exitCode == 0) {
                // Extract version from output
                val version = extractVersion(result.stdout)
                
                LinterDetectionResult(
                    linterName = config.name,
                    isAvailable = true,
                    version = version,
                    installationInstructions = null
                )
            } else {
                LinterDetectionResult(
                    linterName = config.name,
                    isAvailable = false,
                    installationInstructions = getInstallationInstructions(config.name)
                )
            }
        } catch (e: Exception) {
            logger.debug { "Failed to check ${config.name}: ${e.message}" }
            LinterDetectionResult(
                linterName = config.name,
                isAvailable = false,
                installationInstructions = getInstallationInstructions(config.name)
            )
        }
    }
    
    /**
     * Extract version number from version command output
     */
    private fun extractVersion(output: String): String? {
        // Try to find version pattern like "1.2.3" or "v1.2.3"
        val versionPattern = Regex("""v?(\d+\.\d+\.\d+)""")
        val match = versionPattern.find(output)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Get linter configuration by name
     */
    fun getLinterConfig(name: String): LinterConfig? {
        return LINTER_CONFIGS[name]
    }
    
    /**
     * Get all linter configurations
     */
    fun getAllLinterConfigs(): List<LinterConfig> {
        return LINTER_CONFIGS.values.toList()
    }
}

