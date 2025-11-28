package cc.unitmesh.indexer.clustering

import cc.unitmesh.indexer.model.ElementType
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.naming.CommonSuffixRules

/**
 * Clustering strategy based on Git co-change patterns.
 * Files that frequently change together are likely to belong to the same module.
 * 
 * 基于 Git 协变的聚类策略（待实现）：
 * 1. 分析 git commit 历史
 * 2. 提取同时变更的文件
 * 3. 构建协变图进行聚类
 * 
 * 这是一个预留接口，具体实现需要平台特定的 Git 访问能力。
 */
class GitCoChangeClusterer : ClusteringStrategy {
    
    private val suffixRules = CommonSuffixRules()
    
    override val name: String = "git-cochange"
    
    override suspend fun cluster(
        files: List<String>,
        context: ClusteringContext
    ): List<ModuleCluster> {
        // Use pre-provided co-change patterns from context
        if (context.coChangePatterns.isEmpty()) {
            return emptyList()
        }
        
        // Build co-change graph
        val graph = buildCoChangeGraph(files, context.coChangePatterns)
        
        // Find clusters
        return findClusters(files, graph, context)
    }
    
    override fun getStatistics(clusters: List<ModuleCluster>): ClusteringStatistics {
        if (clusters.isEmpty()) {
            return ClusteringStatistics(
                totalFiles = 0,
                clusterCount = 0,
                averageClusterSize = 0f,
                maxClusterSize = 0,
                minClusterSize = 0,
                averageCohesion = 0f,
                averageCoupling = 0f,
                isolatedFilesCount = 0
            )
        }
        
        val allFiles = clusters.flatMap { it.files }.distinct()
        val sizes = clusters.map { it.files.size }
        
        return ClusteringStatistics(
            totalFiles = allFiles.size,
            clusterCount = clusters.size,
            averageClusterSize = sizes.average().toFloat(),
            maxClusterSize = sizes.maxOrNull() ?: 0,
            minClusterSize = sizes.minOrNull() ?: 0,
            averageCohesion = clusters.map { it.cohesion }.average().toFloat(),
            averageCoupling = clusters.map { it.coupling }.average().toFloat(),
            isolatedFilesCount = clusters.count { it.files.size == 1 },
            metadata = mapOf("strategy" to name)
        )
    }
    
    /**
     * Build co-change graph from patterns
     */
    private fun buildCoChangeGraph(
        files: List<String>,
        patterns: Map<String, List<String>>
    ): DependencyGraph {
        val builder = DependencyGraphBuilder()
        val fileSet = files.toSet()
        
        builder.addNodes(files)
        
        for ((file, coChangedFiles) in patterns) {
            if (file !in fileSet) continue
            
            for (coChanged in coChangedFiles) {
                if (coChanged in fileSet && coChanged != file) {
                    builder.addEdge(file, coChanged)
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * Find clusters from co-change graph
     */
    private fun findClusters(
        files: List<String>,
        graph: DependencyGraph,
        context: ClusteringContext
    ): List<ModuleCluster> {
        val visited = mutableSetOf<String>()
        val clusters = mutableListOf<ModuleCluster>()
        var clusterIndex = 0
        
        // Sort by connectivity (most connected first)
        val sortedFiles = files.sortedByDescending { graph.getDegree(it) }
        
        for (file in sortedFiles) {
            if (file in visited) continue
            
            val component = findConnectedComponent(file, graph, visited)
            
            if (component.size >= context.minClusterSize || context.includeIsolatedFiles) {
                val cluster = createCluster(
                    id = "cochange-cluster-${clusterIndex++}",
                    files = component.toList(),
                    graph = graph
                )
                clusters.add(cluster)
            }
        }
        
        return clusters
            .sortedByDescending { it.getImportance() }
            .take(context.maxClusters)
    }
    
    private fun findConnectedComponent(
        startFile: String,
        graph: DependencyGraph,
        visited: MutableSet<String>
    ): Set<String> {
        val component = mutableSetOf<String>()
        val queue = mutableListOf(startFile)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current in visited) continue
            
            visited.add(current)
            component.add(current)
            
            graph.getConnected(current).forEach { neighbor ->
                if (neighbor !in visited && neighbor in graph.nodes) {
                    queue.add(neighbor)
                }
            }
        }
        
        return component
    }
    
    private fun createCluster(
        id: String,
        files: List<String>,
        graph: DependencyGraph
    ): ModuleCluster {
        val coreFiles = files
            .sortedByDescending { graph.getDegree(it) }
            .take(3)
        
        val name = inferClusterName(files, coreFiles)
        val domainTerms = extractDomainTerms(files)
        
        val semanticNames = domainTerms.map { term ->
            SemanticName(
                name = term,
                type = ElementType.CLASS,
                tokens = term.length / 4 + 1,
                source = name,
                original = term,
                weight = 0.6f // Higher weight for co-change based terms
            )
        }
        
        return ModuleCluster(
            id = id,
            name = name,
            files = files,
            coreFiles = coreFiles,
            domainTerms = domainTerms,
            semanticNames = semanticNames,
            cohesion = calculateCohesion(files, graph),
            coupling = calculateCoupling(files, graph),
            metadata = mapOf(
                "fileCount" to files.size.toString(),
                "strategy" to this.name
            )
        )
    }
    
    private fun inferClusterName(files: List<String>, coreFiles: List<String>): String {
        if (coreFiles.isNotEmpty()) {
            val coreFileName = coreFiles.first()
                .substringAfterLast("/")
                .substringBeforeLast(".")
            return suffixRules.normalize(coreFileName).ifEmpty { coreFileName }
        }
        return "Module"
    }
    
    private fun extractDomainTerms(files: List<String>): List<String> {
        val terms = mutableSetOf<String>()
        
        for (filePath in files) {
            val fileName = filePath
                .substringAfterLast("/")
                .substringBeforeLast(".")
            
            val normalized = suffixRules.normalize(fileName)
            val words = CamelCaseSplitter.splitAndFilter(normalized, suffixRules)
            terms.addAll(words.filter { it.length > 2 })
        }
        
        return terms.toList().sortedBy { it }
    }
    
    private fun calculateCohesion(files: List<String>, graph: DependencyGraph): Float {
        if (files.size <= 1) return 1f
        
        val fileSet = files.toSet()
        var edges = 0
        
        for (file in files) {
            edges += graph.getDependencies(file).count { it in fileSet }
        }
        
        val maxEdges = files.size * (files.size - 1)
        return if (maxEdges > 0) (edges.toFloat() / maxEdges).coerceIn(0f, 1f) else 0f
    }
    
    private fun calculateCoupling(files: List<String>, graph: DependencyGraph): Float {
        val fileSet = files.toSet()
        var internal = 0
        var external = 0
        
        for (file in files) {
            val deps = graph.getDependencies(file)
            internal += deps.count { it in fileSet }
            external += deps.count { it !in fileSet }
        }
        
        val total = internal + external
        return if (total > 0) (external.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
}

/**
 * Data class for Git commit info (used by platform-specific implementations)
 */
data class GitCommitInfo(
    val hash: String,
    val files: List<String>,
    val timestamp: Long,
    val author: String = ""
)

/**
 * Interface for Git operations (to be implemented per platform)
 */
interface GitHistoryProvider {
    /**
     * Get recent commits
     */
    suspend fun getRecentCommits(limit: Int = 100): List<GitCommitInfo>
    
    /**
     * Build co-change patterns from commit history
     */
    suspend fun buildCoChangePatterns(
        commits: List<GitCommitInfo>,
        minCoOccurrence: Int = 3
    ): Map<String, List<String>>
}

