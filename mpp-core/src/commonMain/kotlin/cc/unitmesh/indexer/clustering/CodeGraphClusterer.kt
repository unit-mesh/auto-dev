package cc.unitmesh.indexer.clustering

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.model.CodeGraph
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.model.RelationshipType
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.indexer.model.ElementType
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.naming.CommonSuffixRules

/**
 * Clustering strategy using mpp-codegraph for accurate AST-based dependency extraction.
 * Leverages TreeSitter parsing for multi-language support.
 * 
 * 基于 CodeGraph 的聚类策略：
 * 1. 使用 TreeSitter 解析 AST
 * 2. 提取 IMPORT 节点和 DEPENDS_ON 关系
 * 3. 构建精确的依赖图进行聚类
 */
class CodeGraphClusterer(
    private val codeParser: CodeParser
) : ClusteringStrategy {
    
    private val logger = getLogger("CodeGraphClusterer")
    private val suffixRules = CommonSuffixRules()
    private val fallbackClusterer = ImportDependencyClusterer()
    
    override val name: String = "codegraph"
    
    override suspend fun cluster(
        files: List<String>,
        context: ClusteringContext
    ): List<ModuleCluster> {
        // Group files by language
        val filesByLanguage = files.groupBy { detectLanguage(it) }
        
        val allClusters = mutableListOf<ModuleCluster>()
        
        for ((language, languageFiles) in filesByLanguage) {
            if (language == Language.UNKNOWN) {
                // Use fallback for unknown languages
                val fallbackClusters = fallbackClusterer.cluster(languageFiles, context)
                allClusters.addAll(fallbackClusters)
                continue
            }
            
            try {
                // Parse files with CodeGraph
                val fileContents = languageFiles.mapNotNull { filePath ->
                    val content = context.fileContents[filePath]
                    if (content != null) filePath to content else null
                }.toMap()
                
                if (fileContents.isEmpty()) {
                    logger.warn { "No file contents available for language: $language" }
                    continue
                }
                
                val codeGraph = codeParser.parseCodeGraph(fileContents, language)
                
                // Build dependency graph from CodeGraph
                val dependencyGraph = buildDependencyGraph(codeGraph, languageFiles)
                
                // Use the dependency graph for clustering
                val languageClusters = clusterFromGraph(
                    files = languageFiles,
                    dependencyGraph = dependencyGraph,
                    codeGraph = codeGraph,
                    context = context
                )
                
                allClusters.addAll(languageClusters)
                
            } catch (e: Exception) {
                logger.error(e) { "Error parsing files for language $language, using fallback" }
                val fallbackClusters = fallbackClusterer.cluster(languageFiles, context)
                allClusters.addAll(fallbackClusters)
            }
        }
        
        return allClusters
            .sortedByDescending { it.getImportance() }
            .take(context.maxClusters)
    }
    
    override fun getStatistics(clusters: List<ModuleCluster>): ClusteringStatistics {
        return fallbackClusterer.getStatistics(clusters)
    }
    
    /**
     * Build dependency graph from CodeGraph
     */
    private fun buildDependencyGraph(
        codeGraph: CodeGraph,
        files: List<String>
    ): DependencyGraph {
        val builder = DependencyGraphBuilder()
        builder.addNodes(files)
        
        // Get import nodes
        val importNodes = codeGraph.getNodesByType(CodeElementType.IMPORT)
        
        // Get DEPENDS_ON relationships
        val dependsOnRelations = codeGraph.getRelationshipsByType(RelationshipType.DEPENDS_ON)
        
        // Map nodes to files
        val nodeToFile = mutableMapOf<String, String>()
        for (node in codeGraph.nodes) {
            nodeToFile[node.id] = node.filePath
        }
        
        // Add edges from relationships
        for (relation in dependsOnRelations) {
            val sourceFile = nodeToFile[relation.sourceId]
            val targetFile = nodeToFile[relation.targetId]
            
            if (sourceFile != null && targetFile != null && sourceFile != targetFile) {
                builder.addEdge(sourceFile, targetFile)
            }
        }
        
        // Also extract imports from import nodes
        for (importNode in importNodes) {
            val sourceFile = importNode.filePath
            val importedName = importNode.name
            
            // Try to resolve import to a file
            val targetFile = resolveImportToFile(importedName, importNode.qualifiedName, files)
            if (targetFile != null && targetFile != sourceFile) {
                builder.addEdge(sourceFile, targetFile)
            }
        }
        
        return builder.build()
    }
    
    /**
     * Resolve import name to actual file path
     */
    private fun resolveImportToFile(
        importName: String,
        qualifiedName: String,
        files: List<String>
    ): String? {
        // Convert qualified name to path patterns
        val pathPattern = qualifiedName
            .replace(".", "/")
            .replace("::", "/")
        
        // Find matching file
        return files.find { file ->
            file.contains(pathPattern) ||
            file.endsWith("$pathPattern.kt") ||
            file.endsWith("$pathPattern.java") ||
            file.endsWith("$pathPattern.py") ||
            file.endsWith("$pathPattern.js") ||
            file.endsWith("$pathPattern.ts")
        }
    }
    
    /**
     * Cluster files using dependency graph
     */
    private fun clusterFromGraph(
        files: List<String>,
        dependencyGraph: DependencyGraph,
        codeGraph: CodeGraph,
        context: ClusteringContext
    ): List<ModuleCluster> {
        val visited = mutableSetOf<String>()
        val clusters = mutableListOf<ModuleCluster>()
        var clusterIndex = 0
        
        // Sort files by degree (hub files first)
        val sortedFiles = files.sortedByDescending { dependencyGraph.getDegree(it) }
        
        for (file in sortedFiles) {
            if (file in visited) continue
            
            // Find connected component
            val component = findConnectedComponent(file, dependencyGraph, visited)
            
            if (component.size >= context.minClusterSize || context.includeIsolatedFiles) {
                val cluster = createClusterWithCodeGraph(
                    id = "codegraph-cluster-${clusterIndex++}",
                    files = component.toList(),
                    dependencyGraph = dependencyGraph,
                    codeGraph = codeGraph
                )
                clusters.add(cluster)
            }
        }
        
        return clusters
    }
    
    /**
     * Find connected component using BFS
     */
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
    
    /**
     * Create cluster with additional info from CodeGraph
     */
    private fun createClusterWithCodeGraph(
        id: String,
        files: List<String>,
        dependencyGraph: DependencyGraph,
        codeGraph: CodeGraph
    ): ModuleCluster {
        // Get code nodes for these files
        val fileNodes = codeGraph.nodes.filter { it.filePath in files }
        
        // Find core files (highest connectivity)
        val coreFiles = files
            .sortedByDescending { dependencyGraph.getDegree(it) }
            .take(3)
        
        // Extract domain terms from class and method names
        val domainTerms = extractDomainTermsFromNodes(fileNodes)
        
        // Infer cluster name
        val name = inferClusterNameFromNodes(files, fileNodes, coreFiles)
        
        // Create semantic names from nodes
        val semanticNames = createSemanticNamesFromNodes(fileNodes, name)
        
        // Calculate metrics
        val cohesion = calculateCohesion(files, dependencyGraph)
        val coupling = calculateCoupling(files, dependencyGraph)
        
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
                "nodeCount" to fileNodes.size.toString(),
                "strategy" to this.name
            )
        )
    }
    
    /**
     * Extract domain terms from code nodes
     */
    private fun extractDomainTermsFromNodes(nodes: List<CodeNode>): List<String> {
        val terms = mutableSetOf<String>()
        
        for (node in nodes) {
            when (node.type) {
                CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                    val normalized = suffixRules.normalize(node.name)
                    val words = CamelCaseSplitter.splitAndFilter(normalized, suffixRules)
                    terms.addAll(words.filter { it.length > 2 })
                }
                CodeElementType.METHOD, CodeElementType.FUNCTION -> {
                    // Extract domain terms from method names (less weight)
                    val words = CamelCaseSplitter.split(node.name)
                    terms.addAll(words.filter { it.length > 3 && !isCommonVerb(it) })
                }
                else -> { /* Skip other types */ }
            }
        }
        
        return terms.toList().sortedBy { it }
    }
    
    /**
     * Infer cluster name from nodes
     */
    private fun inferClusterNameFromNodes(
        files: List<String>,
        nodes: List<CodeNode>,
        coreFiles: List<String>
    ): String {
        // Try to find a common package/namespace
        val packages = nodes
            .map { it.packageName }
            .filter { it.isNotEmpty() }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.key
        
        if (packages != null) {
            val lastSegment = packages.split(".").lastOrNull { it.isNotEmpty() }
            if (lastSegment != null && lastSegment.length > 2) {
                return lastSegment.replaceFirstChar { it.uppercase() }
            }
        }
        
        // Use core class name
        val coreClasses = nodes
            .filter { it.filePath in coreFiles && it.type == CodeElementType.CLASS }
            .sortedByDescending { it.content.length }
        
        if (coreClasses.isNotEmpty()) {
            return suffixRules.normalize(coreClasses.first().name).ifEmpty { 
                coreClasses.first().name 
            }
        }
        
        // Fallback to common path
        return findCommonPathName(files)
    }
    
    /**
     * Create semantic names from code nodes
     */
    private fun createSemanticNamesFromNodes(
        nodes: List<CodeNode>,
        clusterName: String
    ): List<SemanticName> {
        return nodes
            .filter { it.type in listOf(CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM) }
            .map { node ->
                val normalized = suffixRules.normalize(node.name)
                SemanticName(
                    name = normalized.ifEmpty { node.name },
                    type = mapCodeElementType(node.type),
                    tokens = node.name.length / 4 + 1,
                    source = clusterName,
                    original = node.name,
                    weight = calculateNodeWeight(node),
                    packageName = node.packageName
                )
            }
    }
    
    /**
     * Calculate weight for a code node
     */
    private fun calculateNodeWeight(node: CodeNode): Float {
        var weight = 0.5f
        
        // Type weight
        weight += when (node.type) {
            CodeElementType.INTERFACE -> 0.2f
            CodeElementType.CLASS -> 0.1f
            CodeElementType.ENUM -> 0.05f
            else -> 0f
        }
        
        // Size weight (more content = more important)
        weight += when {
            node.content.length > 2000 -> 0.15f
            node.content.length > 500 -> 0.1f
            node.content.length > 100 -> 0.05f
            else -> 0f
        }
        
        return weight.coerceIn(0f, 1f)
    }
    
    private fun mapCodeElementType(type: CodeElementType): ElementType {
        return when (type) {
            CodeElementType.CLASS -> ElementType.CLASS
            CodeElementType.INTERFACE -> ElementType.INTERFACE
            CodeElementType.ENUM -> ElementType.ENUM
            CodeElementType.METHOD, CodeElementType.FUNCTION -> ElementType.METHOD
            CodeElementType.FIELD, CodeElementType.PROPERTY -> ElementType.FIELD
            else -> ElementType.UNKNOWN
        }
    }
    
    private fun findCommonPathName(files: List<String>): String {
        if (files.isEmpty()) return "Module"
        
        val segments = files.map { it.split("/") }
        val minLen = segments.minOfOrNull { it.size } ?: 0
        
        for (i in (minLen - 1) downTo 0) {
            val segment = segments.first()[i]
            if (segments.all { it[i] == segment } && segment.length > 2) {
                return segment.replaceFirstChar { it.uppercase() }
            }
        }
        
        return "Module"
    }
    
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
    
    private fun detectLanguage(filePath: String): Language {
        val extension = filePath.substringAfterLast(".", "").lowercase()
        return when (extension) {
            "java" -> Language.JAVA
            "kt", "kts" -> Language.KOTLIN
            "py" -> Language.PYTHON
            "js", "jsx" -> Language.JAVASCRIPT
            "ts", "tsx" -> Language.TYPESCRIPT
            "go" -> Language.GO
            "rs" -> Language.RUST
            "cs" -> Language.CSHARP
            else -> Language.UNKNOWN
        }
    }
    
    private fun isCommonVerb(word: String): Boolean {
        return word.lowercase() in setOf(
            "get", "set", "is", "has", "can", "should", "will", "do", "make",
            "create", "update", "delete", "find", "search", "list", "add",
            "remove", "check", "validate", "convert", "parse", "format"
        )
    }
}

