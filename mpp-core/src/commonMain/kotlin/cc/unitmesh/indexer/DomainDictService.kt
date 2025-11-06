package cc.unitmesh.indexer

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.indexer.model.DomainDictionary
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.model.ElementType
import cc.unitmesh.indexer.naming.CommonSuffixRules
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.scoring.FileWeightCalculator
import cc.unitmesh.indexer.utils.TokenCounter
import cc.unitmesh.devins.filesystem.ProjectFileSystem

/**
 * Service for collecting and managing domain dictionaries from code.
 * Integrates with mpp-codegraph to extract semantic information across different programming languages.
 */
class DomainDictService(
    private val fileSystem: ProjectFileSystem,
    private val baseDir: String = "prompts"
) {
    private val logger = getLogger("DomainDictService")
    private val suffixRules = CommonSuffixRules()
    private val tokenCounter = TokenCounter.DEFAULT
    
    /**
     * Collect semantic names from the project files
     * Simplified implementation that extracts names from file paths
     */
    suspend fun collectSemanticNames(maxTokenLength: Int = 4096): DomainDictionary {
        val files = getProjectFiles()

        val level1 = collectLevel1Names(files, maxTokenLength / 2)
        val level2 = emptyList<SemanticName>() // Simplified: no Level 2 for now

        val metadata = mapOf(
            "level1_count" to level1.size.toString(),
            "level2_count" to level2.size.toString(),
            "total_tokens" to level1.sumOf { it.tokens }.toString(),
            "max_tokens" to maxTokenLength.toString()
        )

        return DomainDictionary(level1, level2, metadata)
    }
    
    /**
     * Load existing domain dictionary content from file
     */
    suspend fun loadContent(): String? {
        return try {
            val dictFile = "$baseDir/domain.csv"
            if (fileSystem.exists(dictFile)) {
                fileSystem.readFile(dictFile)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading domain dictionary: ${e.message}" }
            null
        }
    }
    
    /**
     * Save domain dictionary content to file
     */
    suspend fun saveContent(content: String): Boolean {
        return try {
            // Ensure the prompts directory exists
            if (!fileSystem.exists(baseDir)) {
                fileSystem.createDirectory(baseDir)
            }
            
            val dictFile = "$baseDir/domain.csv"
            fileSystem.writeFile(dictFile, content)
            true
        } catch (e: Exception) {
            logger.error(e) { "Error saving domain dictionary: ${e.message}" }
            false
        }
    }
    
    /**
     * Get project files for analysis
     */
    private suspend fun getProjectFiles(): List<String> {
        return try {
            val allFiles = fileSystem.searchFiles("*", maxDepth = 10, maxResults = 1000)
            allFiles.filter { shouldIncludeFile(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error listing project files: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Collect Level 1 semantic names from file paths
     */
    private fun collectLevel1Names(files: List<String>, maxTokens: Int): List<SemanticName> {
        val names = mutableListOf<SemanticName>()
        var tokenUsed = 0

        for (filePath in files) {
            if (tokenUsed >= maxTokens) break

            val fileName = filePath.substringAfterLast('/').substringBeforeLast('.')
            val normalized = suffixRules.normalize(fileName)

            if (normalized.isEmpty()) continue

            // Calculate weight based on file characteristics
            val weight = FileWeightCalculator.calculateWeight(
                filePath = filePath,
                fileSize = 0,
                isInMainSource = !filePath.contains("/test/"),
                isTestFile = filePath.contains("/test/") || fileName.contains("Test")
            )

            val category = FileWeightCalculator.getWeightCategory(weight)

            // Split camelCase names into words
            val words = CamelCaseSplitter.splitAndFilter(normalized, suffixRules)

            for (word in words) {
                if (word.isNotEmpty()) {
                    val tokenCost = tokenCounter.countTokens(word)
                    if (tokenUsed + tokenCost > maxTokens) break

                    names.add(
                        SemanticName(
                            name = word,
                            type = ElementType.FILE,
                            tokens = tokenCost,
                            source = fileName,
                            original = fileName,
                            weight = weight,
                            weightCategory = category
                        )
                    )

                    tokenUsed += tokenCost
                }
            }
        }

        return names.distinctBy { it.name }
    }
    
    /**
     * Detect if file is a source code file
     */
    private fun isSourceFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return extension in setOf("java", "kt", "kts", "js", "jsx", "ts", "tsx", "py", "cs", "cpp", "c", "h", "hpp")
    }
    
    /**
     * Determine if a file should be included in analysis
     */
    private fun shouldIncludeFile(filePath: String): Boolean {
        val fileName = filePath.substringAfterLast('/')

        // Skip hidden files, build outputs, and test files
        return isSourceFile(filePath) &&
               !fileName.startsWith(".") &&
               !filePath.contains("/build/") &&
               !filePath.contains("/target/") &&
               !filePath.contains("/node_modules/") &&
               !filePath.contains("/.gradle/") &&
               !filePath.contains("/bin/") &&
               !filePath.contains("/out/") &&
               !filePath.contains("/dist/") &&
               !filePath.contains("/generated/") &&
               !filePath.contains("/test/") &&
               !filePath.contains("/tests/") &&
               !fileName.contains("Test.") &&
               !fileName.contains("Spec.") &&
               !fileName.endsWith(".min.js") &&
               !fileName.endsWith(".d.ts")
    }
    
    /**
     * Get README content for context
     */
    suspend fun getReadmeContent(): String {
        val readmeVariations = listOf(
            "README.md", "Readme.md", "readme.md",
            "README.txt", "Readme.txt", "readme.txt",
            "README", "Readme", "readme"
        )

        for (readmeFile in readmeVariations) {
            try {
                if (fileSystem.exists(readmeFile)) {
                    return fileSystem.readFile(readmeFile) ?: ""
                }
            } catch (e: Exception) {
                // Continue trying other variations
            }
        }

        return ""
    }
}
