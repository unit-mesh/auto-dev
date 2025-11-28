package cc.unitmesh.indexer.clustering

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.indexer.model.ElementType
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.naming.CommonSuffixRules

/**
 * Clustering strategy based on import dependencies.
 * Uses code graph to extract import relationships and cluster related files.
 * 
 * 基于 Import 依赖的聚类策略：
 * 1. 解析代码提取 import 语句
 * 2. 构建依赖图
 * 3. 使用连通分量算法聚类
 */
class ImportDependencyClusterer : ClusteringStrategy {
    
    private val logger = getLogger("ImportDependencyClusterer")
    private val suffixRules = CommonSuffixRules()
    
    override val name: String = "import-dependency"
    
    override suspend fun cluster(
        files: List<String>,
        context: ClusteringContext
    ): List<ModuleCluster> {
        // Use pre-built dependency graph if available, otherwise build from file contents
        val dependencyGraph = context.dependencyGraph 
            ?: buildDependencyGraphFromContents(files, context.fileContents)
        
        if (dependencyGraph.nodes.isEmpty()) {
            logger.warn { "No dependency graph available for clustering" }
            return emptyList()
        }
        
        // Find connected components (clusters)
        val clusters = findConnectedComponents(dependencyGraph, context)
        
        // Filter and sort clusters
        return clusters
            .filter { it.files.size >= context.minClusterSize || context.includeIsolatedFiles }
            .sortedByDescending { it.getImportance() }
            .take(context.maxClusters)
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
        val isolatedCount = clusters.count { it.files.size == 1 }
        
        return ClusteringStatistics(
            totalFiles = allFiles.size,
            clusterCount = clusters.size,
            averageClusterSize = sizes.average().toFloat(),
            maxClusterSize = sizes.maxOrNull() ?: 0,
            minClusterSize = sizes.minOrNull() ?: 0,
            averageCohesion = clusters.map { it.cohesion }.average().toFloat(),
            averageCoupling = clusters.map { it.coupling }.average().toFloat(),
            isolatedFilesCount = isolatedCount
        )
    }
    
    /**
     * Build dependency graph from file contents by extracting import statements
     */
    private fun buildDependencyGraphFromContents(
        files: List<String>,
        fileContents: Map<String, String>
    ): DependencyGraph {
        val builder = DependencyGraphBuilder()
        builder.addNodes(files)
        
        for (filePath in files) {
            val content = fileContents[filePath] ?: continue
            val language = detectLanguage(filePath)
            
            val imports = extractImports(content, language)
            
            // Resolve imports to actual files in the project
            for (import in imports) {
                val resolvedFile = resolveImportToFile(import, files, filePath)
                if (resolvedFile != null && resolvedFile != filePath) {
                    builder.addEdge(filePath, resolvedFile)
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * Extract import statements from source code
     * Multi-language support: Java, Kotlin, Python, JavaScript/TypeScript
     */
    private fun extractImports(content: String, language: String): List<String> {
        return when (language) {
            "java", "kotlin" -> extractJvmImports(content)
            "python" -> extractPythonImports(content)
            "javascript", "typescript" -> extractJsImports(content)
            "go" -> extractGoImports(content)
            "rust" -> extractRustImports(content)
            else -> emptyList()
        }
    }
    
    private fun extractJvmImports(content: String): List<String> {
        val importRegex = Regex("""import\s+(static\s+)?([a-zA-Z_][\w.]*[\w*])""")
        return importRegex.findAll(content)
            .map { it.groupValues[2] }
            .toList()
    }
    
    private fun extractPythonImports(content: String): List<String> {
        val imports = mutableListOf<String>()
        
        // from X import Y
        val fromImportRegex = Regex("""from\s+([\w.]+)\s+import\s+(.+)""")
        fromImportRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        // import X
        val importRegex = Regex("""^import\s+([\w.]+)""", RegexOption.MULTILINE)
        importRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        return imports
    }
    
    private fun extractJsImports(content: String): List<String> {
        val imports = mutableListOf<String>()
        
        // ES6: import X from 'Y'
        val es6ImportRegex = Regex("""import\s+(?:.+\s+from\s+)?['"]([@\w./-]+)['"]""")
        es6ImportRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        // CommonJS: require('X')
        val requireRegex = Regex("""require\s*\(\s*['"]([@\w./-]+)['"]\s*\)""")
        requireRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        return imports
    }
    
    private fun extractGoImports(content: String): List<String> {
        val importRegex = Regex("""import\s*\(\s*([\s\S]*?)\s*\)|import\s+"([^"]+)"""")
        val imports = mutableListOf<String>()
        
        importRegex.findAll(content).forEach { match ->
            if (match.groupValues[1].isNotEmpty()) {
                // Multi-line import block
                val block = match.groupValues[1]
                val lineRegex = Regex(""""([^"]+)"""")
                lineRegex.findAll(block).forEach { lineMatch ->
                    imports.add(lineMatch.groupValues[1])
                }
            } else if (match.groupValues[2].isNotEmpty()) {
                imports.add(match.groupValues[2])
            }
        }
        
        return imports
    }
    
    private fun extractRustImports(content: String): List<String> {
        val useRegex = Regex("""use\s+([\w:]+)""")
        return useRegex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }
    
    /**
     * Resolve an import path to an actual file in the project
     */
    private fun resolveImportToFile(
        importPath: String,
        projectFiles: List<String>,
        currentFile: String
    ): String? {
        // Convert import path to potential file patterns
        val patterns = generateFilePatterns(importPath)
        
        // Find matching file in project
        for (pattern in patterns) {
            val match = projectFiles.find { file ->
                file.endsWith(pattern) || file.contains(pattern)
            }
            if (match != null) return match
        }
        
        return null
    }
    
    /**
     * Generate potential file path patterns from import path
     */
    private fun generateFilePatterns(importPath: String): List<String> {
        val patterns = mutableListOf<String>()
        
        // Clean import path
        val cleanPath = importPath
            .replace(".", "/")
            .replace("::", "/")
            .removePrefix("@")
        
        // Common file extensions
        val extensions = listOf("", ".kt", ".java", ".py", ".js", ".ts", ".tsx", ".go", ".rs")
        
        for (ext in extensions) {
            patterns.add("$cleanPath$ext")
            
            // Also try with last segment as class name
            val segments = cleanPath.split("/")
            if (segments.size > 1) {
                val lastSegment = segments.last()
                patterns.add("$cleanPath/$lastSegment$ext")
            }
        }
        
        return patterns
    }
    
    /**
     * Find connected components in the dependency graph
     */
    private fun findConnectedComponents(
        graph: DependencyGraph,
        context: ClusteringContext
    ): List<ModuleCluster> {
        val visited = mutableSetOf<String>()
        val clusters = mutableListOf<ModuleCluster>()
        var clusterIndex = 0
        
        for (node in graph.nodes) {
            if (node in visited) continue
            
            // BFS to find connected component
            val component = mutableSetOf<String>()
            val queue = mutableListOf(node)
            
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current in visited) continue
                
                visited.add(current)
                component.add(current)
                
                // Add all connected nodes
                graph.getConnected(current).forEach { neighbor ->
                    if (neighbor !in visited && neighbor in graph.nodes) {
                        queue.add(neighbor)
                    }
                }
            }
            
            if (component.isNotEmpty()) {
                val cluster = createCluster(
                    id = "cluster-${clusterIndex++}",
                    files = component.toList(),
                    graph = graph
                )
                clusters.add(cluster)
            }
        }
        
        return clusters
    }
    
    /**
     * Create a module cluster from a set of files
     */
    private fun createCluster(
        id: String,
        files: List<String>,
        graph: DependencyGraph
    ): ModuleCluster {
        // Find core files (highest connectivity within cluster)
        val coreFiles = files
            .sortedByDescending { file ->
                graph.getDependencies(file).count { it in files } +
                graph.getDependents(file).count { it in files }
            }
            .take(3)
        
        // Infer cluster name from common path prefix or core file names
        val name = inferClusterName(files, coreFiles)
        
        // Extract domain terms from file names
        val domainTerms = extractDomainTerms(files)
        
        // Create semantic names
        val semanticNames = domainTerms.map { term ->
            SemanticName(
                name = term,
                type = ElementType.CLASS,
                tokens = term.length / 4 + 1,
                source = name,
                original = term,
                weight = 0.5f
            )
        }
        
        // Calculate cohesion (internal edges / possible internal edges)
        val cohesion = calculateCohesion(files, graph)
        
        // Calculate coupling (external edges / total edges)
        val coupling = calculateCoupling(files, graph)
        
        return ModuleCluster(
            id = id,
            name = name,
            files = files,
            coreFiles = coreFiles,
            domainTerms = domainTerms,
            semanticNames = semanticNames,
            cohesion = cohesion,
            coupling = coupling,
            metadata = mapOf(
                "fileCount" to files.size.toString(),
                "strategy" to this.name
            )
        )
    }
    
    /**
     * Infer a meaningful name for the cluster
     */
    private fun inferClusterName(files: List<String>, coreFiles: List<String>): String {
        // Try to find common path prefix
        val commonPrefix = findCommonPathPrefix(files)
        if (commonPrefix.isNotEmpty()) {
            val lastSegment = commonPrefix.split("/").lastOrNull { it.isNotEmpty() }
            if (lastSegment != null && lastSegment.length > 2) {
                return lastSegment.replaceFirstChar { it.uppercase() }
            }
        }
        
        // Use core file name
        if (coreFiles.isNotEmpty()) {
            val coreFileName = coreFiles.first()
                .substringAfterLast("/")
                .substringBeforeLast(".")
            return suffixRules.normalize(coreFileName).ifEmpty { coreFileName }
        }
        
        return "Module"
    }
    
    /**
     * Find common path prefix among files
     */
    private fun findCommonPathPrefix(files: List<String>): String {
        if (files.isEmpty()) return ""
        if (files.size == 1) return files.first().substringBeforeLast("/")
        
        val paths = files.map { it.split("/") }
        val minLength = paths.minOfOrNull { it.size } ?: 0
        
        val commonParts = mutableListOf<String>()
        for (i in 0 until minLength) {
            val part = paths.first()[i]
            if (paths.all { it[i] == part }) {
                commonParts.add(part)
            } else {
                break
            }
        }
        
        return commonParts.joinToString("/")
    }
    
    /**
     * Extract domain terms from file names in the cluster
     */
    private fun extractDomainTerms(files: List<String>): List<String> {
        val terms = mutableSetOf<String>()
        
        for (filePath in files) {
            val fileName = filePath
                .substringAfterLast("/")
                .substringBeforeLast(".")
            
            // Normalize and split
            val normalized = suffixRules.normalize(fileName)
            val words = CamelCaseSplitter.splitAndFilter(normalized, suffixRules)
            
            terms.addAll(words.filter { it.length > 2 })
        }
        
        return terms.toList().sortedBy { it }
    }
    
    /**
     * Calculate cohesion: ratio of internal edges to possible internal edges
     */
    private fun calculateCohesion(files: List<String>, graph: DependencyGraph): Float {
        if (files.size <= 1) return 1f
        
        val fileSet = files.toSet()
        var internalEdges = 0
        
        for (file in files) {
            internalEdges += graph.getDependencies(file).count { it in fileSet }
        }
        
        val maxPossibleEdges = files.size * (files.size - 1)
        return if (maxPossibleEdges > 0) {
            (internalEdges.toFloat() / maxPossibleEdges).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Calculate coupling: ratio of external edges to total edges
     */
    private fun calculateCoupling(files: List<String>, graph: DependencyGraph): Float {
        val fileSet = files.toSet()
        var internalEdges = 0
        var externalEdges = 0
        
        for (file in files) {
            val deps = graph.getDependencies(file)
            internalEdges += deps.count { it in fileSet }
            externalEdges += deps.count { it !in fileSet }
            
            val dependents = graph.getDependents(file)
            externalEdges += dependents.count { it !in fileSet }
        }
        
        val totalEdges = internalEdges + externalEdges
        return if (totalEdges > 0) {
            (externalEdges.toFloat() / totalEdges).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Detect programming language from file extension
     */
    private fun detectLanguage(filePath: String): String {
        val extension = filePath.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "java" -> "java"
            "kt", "kts" -> "kotlin"
            "py" -> "python"
            "js", "jsx" -> "javascript"
            "ts", "tsx" -> "typescript"
            "go" -> "go"
            "rs" -> "rust"
            "cs" -> "csharp"
            else -> "unknown"
        }
    }
}

