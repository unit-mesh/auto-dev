package cc.unitmesh.indexer.clustering

import cc.unitmesh.indexer.model.SemanticName
import kotlinx.serialization.Serializable

/**
 * Abstract interface for clustering code elements based on different strategies.
 * Supports multiple data sources: import dependencies, git co-change patterns, etc.
 * 
 * 聚类策略抽象接口：
 * - Import 依赖聚类：基于代码依赖关系
 * - Git 协变聚类：基于 git commit 中同时变更的文件（未来扩展）
 * - 语义聚类：基于代码语义相似性（未来扩展）
 */
interface ClusteringStrategy {
    
    /**
     * Strategy name for identification
     */
    val name: String
    
    /**
     * Cluster files based on this strategy
     * 
     * @param files List of file paths to cluster
     * @param context Additional context for clustering (e.g., file contents, git history)
     * @return List of module clusters
     */
    suspend fun cluster(
        files: List<String>,
        context: ClusteringContext
    ): List<ModuleCluster>
    
    /**
     * Get clustering statistics
     */
    fun getStatistics(clusters: List<ModuleCluster>): ClusteringStatistics
}

/**
 * Context for clustering operations
 * Provides necessary information for different clustering strategies
 */
@Serializable
data class ClusteringContext(
    /**
     * Map of file path to file content (optional, for semantic analysis)
     */
    val fileContents: Map<String, String> = emptyMap(),
    
    /**
     * Pre-parsed dependency graph (optional)
     */
    val dependencyGraph: DependencyGraph? = null,
    
    /**
     * Git co-change data: file -> list of files that often change together (optional)
     */
    val coChangePatterns: Map<String, List<String>> = emptyMap(),
    
    /**
     * Maximum number of clusters to generate
     */
    val maxClusters: Int = 20,
    
    /**
     * Minimum cluster size (files count)
     */
    val minClusterSize: Int = 2,
    
    /**
     * Whether to include isolated files in clusters
     */
    val includeIsolatedFiles: Boolean = false
)

/**
 * Represents a module/cluster of related files
 * 模块聚类结果：包含相关文件和领域名词
 */
@Serializable
data class ModuleCluster(
    /**
     * Cluster unique identifier
     */
    val id: String,
    
    /**
     * Inferred module name based on common patterns
     */
    val name: String,
    
    /**
     * Files belonging to this cluster
     */
    val files: List<String>,
    
    /**
     * Core/central files in this cluster (high connectivity)
     */
    val coreFiles: List<String> = emptyList(),
    
    /**
     * Extracted domain terms from this cluster
     */
    val domainTerms: List<String> = emptyList(),
    
    /**
     * Semantic names extracted from this cluster
     */
    val semanticNames: List<SemanticName> = emptyList(),
    
    /**
     * Cluster cohesion score [0.0, 1.0]
     * Higher = more tightly connected
     */
    val cohesion: Float = 0.0f,
    
    /**
     * Coupling with other clusters [0.0, 1.0]
     * Lower = more independent
     */
    val coupling: Float = 0.0f,
    
    /**
     * Cluster metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get estimated importance based on size and cohesion
     */
    fun getImportance(): Float {
        val sizeWeight = (files.size.toFloat() / 10f).coerceAtMost(1f)
        return (cohesion * 0.6f + sizeWeight * 0.4f).coerceIn(0f, 1f)
    }
}

/**
 * Statistics for clustering results
 */
@Serializable
data class ClusteringStatistics(
    val totalFiles: Int,
    val clusterCount: Int,
    val averageClusterSize: Float,
    val maxClusterSize: Int,
    val minClusterSize: Int,
    val averageCohesion: Float,
    val averageCoupling: Float,
    val isolatedFilesCount: Int,
    val metadata: Map<String, String> = emptyMap()
)

