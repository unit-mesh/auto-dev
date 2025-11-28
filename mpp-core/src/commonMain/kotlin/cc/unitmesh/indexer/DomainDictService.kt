package cc.unitmesh.indexer

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.indexer.clustering.ClusteringContext
import cc.unitmesh.indexer.clustering.ClusteringStatistics
import cc.unitmesh.indexer.clustering.ClusteringStrategy
import cc.unitmesh.indexer.clustering.ImportDependencyClusterer
import cc.unitmesh.indexer.clustering.ModuleCluster
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
 * 
 * Enhanced with clustering analysis for better module understanding:
 * - Import dependency clustering: Groups files by code dependencies
 * - Git co-change clustering: Groups files that change together (future)
 */
class DomainDictService(
    private val fileSystem: ProjectFileSystem,
    private val baseDir: String = "prompts",
    private val clusteringStrategy: ClusteringStrategy = ImportDependencyClusterer()
) {
    private val logger = getLogger("DomainDictService")
    private val suffixRules = CommonSuffixRules()
    private val tokenCounter = TokenCounter.DEFAULT
    
    /**
     * Collect semantic names from the project files
     * Simplified implementation that extracts names from file paths
     */
    suspend fun collectSemanticNames(maxTokenLength: Int = 128000): DomainDictionary {
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
    
    // ============= Clustering Analysis =============
    
    /**
     * Perform clustering analysis on project files
     * Returns module clusters with domain terms
     */
    suspend fun clusterFiles(
        maxClusters: Int = 20,
        minClusterSize: Int = 2,
        includeIsolatedFiles: Boolean = false
    ): List<ModuleCluster> {
        val files = getProjectFiles()
        if (files.isEmpty()) {
            logger.warn { "No files found for clustering analysis" }
            return emptyList()
        }
        
        // Read file contents for dependency analysis
        val fileContents = mutableMapOf<String, String>()
        for (filePath in files) {
            try {
                val content = fileSystem.readFile(filePath)
                if (content != null) {
                    fileContents[filePath] = content
                }
            } catch (e: Exception) {
                logger.debug { "Could not read file for clustering: $filePath" }
            }
        }
        
        val context = ClusteringContext(
            fileContents = fileContents,
            maxClusters = maxClusters,
            minClusterSize = minClusterSize,
            includeIsolatedFiles = includeIsolatedFiles
        )
        
        return try {
            val clusters = clusteringStrategy.cluster(files, context)
            logger.info { "Clustering completed: ${clusters.size} clusters from ${files.size} files" }
            clusters
        } catch (e: Exception) {
            logger.error(e) { "Clustering analysis failed: ${e.message}" }
            emptyList()
        }
    }
    
    /**
     * Get clustering statistics
     */
    fun getClusteringStatistics(clusters: List<ModuleCluster>): ClusteringStatistics {
        return clusteringStrategy.getStatistics(clusters)
    }
    
    /**
     * Collect semantic names with clustering enhancement
     * Clusters are used to add module context and improve weight calculation
     */
    suspend fun collectSemanticNamesWithClustering(
        maxTokenLength: Int = 128000,
        maxClusters: Int = 20
    ): ClusteredDomainDictionary {
        // First, perform clustering
        val clusters = clusterFiles(maxClusters = maxClusters)
        
        // Collect base semantic names
        val baseDictionary = collectSemanticNames(maxTokenLength)
        
        // Enhance with cluster information
        val enhancedLevel1 = enhanceNamesWithClusters(baseDictionary.level1, clusters)
        val enhancedLevel2 = enhanceNamesWithClusters(baseDictionary.level2, clusters)
        
        // Extract additional domain terms from clusters
        val clusterTerms = clusters.flatMap { cluster ->
            cluster.semanticNames.map { name ->
                name.copy(
                    source = "cluster:${cluster.name}",
                    weight = name.weight * cluster.cohesion.coerceAtLeast(0.5f)
                )
            }
        }.distinctBy { it.name }
        
        val metadata = baseDictionary.metadata.toMutableMap()
        metadata["cluster_count"] = clusters.size.toString()
        metadata["cluster_terms"] = clusterTerms.size.toString()
        
        return ClusteredDomainDictionary(
            level1 = enhancedLevel1,
            level2 = enhancedLevel2,
            clusters = clusters,
            clusterTerms = clusterTerms,
            metadata = metadata
        )
    }
    
    /**
     * Enhance semantic names with cluster context
     */
    private fun enhanceNamesWithClusters(
        names: List<SemanticName>,
        clusters: List<ModuleCluster>
    ): List<SemanticName> {
        // Build file-to-cluster mapping
        val fileToCluster = mutableMapOf<String, ModuleCluster>()
        for (cluster in clusters) {
            for (file in cluster.files) {
                fileToCluster[file] = cluster
            }
        }
        
        return names.map { name ->
            val cluster = fileToCluster[name.source]
            if (cluster != null) {
                // Boost weight for names in high-cohesion clusters
                val clusterBoost = cluster.cohesion * 0.2f
                name.copy(
                    weight = (name.weight + clusterBoost).coerceAtMost(1f),
                    packageName = if (name.packageName.isEmpty()) cluster.name else name.packageName
                )
            } else {
                name
            }
        }
    }
    
    /**
     * Format clusters as summary for LLM context
     */
    fun formatClustersForLLM(clusters: List<ModuleCluster>, maxTokens: Int = 2000): String {
        val sb = StringBuilder()
        sb.appendLine("# Module Structure")
        sb.appendLine()
        
        var tokenCount = 50 // Reserve for header
        
        for (cluster in clusters.sortedByDescending { it.getImportance() }) {
            val clusterText = buildString {
                appendLine("## ${cluster.name}")
                appendLine("- Files: ${cluster.files.size}")
                appendLine("- Core: ${cluster.coreFiles.take(3).joinToString(", ") { it.substringAfterLast("/") }}")
                if (cluster.domainTerms.isNotEmpty()) {
                    appendLine("- Domain terms: ${cluster.domainTerms.take(10).joinToString(", ")}")
                }
                appendLine()
            }
            
            val textTokens = tokenCounter.countTokens(clusterText)
            if (tokenCount + textTokens > maxTokens) break
            
            sb.append(clusterText)
            tokenCount += textTokens
        }
        
        return sb.toString()
    }
}

/**
 * Domain dictionary with clustering information
 */
data class ClusteredDomainDictionary(
    val level1: List<SemanticName>,
    val level2: List<SemanticName>,
    val clusters: List<ModuleCluster>,
    val clusterTerms: List<SemanticName>,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get all semantic names including cluster terms
     */
    fun getAllNames(): List<String> {
        return (level1 + level2 + clusterTerms)
            .map { it.name }
            .distinct()
    }
    
    /**
     * Get names sorted by weight
     */
    fun getAllNamesSortedByWeight(): List<String> {
        return (level1 + level2 + clusterTerms)
            .distinctBy { it.name }
            .sortedByDescending { it.weight }
            .map { it.name }
    }
    
    /**
     * Get total token count
     */
    fun getTotalTokens(): Int {
        return (level1 + level2 + clusterTerms).sumOf { it.tokens }
    }
    
    /**
     * Convert to basic DomainDictionary (for backward compatibility)
     */
    fun toDomainDictionary(): DomainDictionary {
        return DomainDictionary(
            level1 = level1 + clusterTerms,
            level2 = level2,
            metadata = metadata
        )
    }
}
