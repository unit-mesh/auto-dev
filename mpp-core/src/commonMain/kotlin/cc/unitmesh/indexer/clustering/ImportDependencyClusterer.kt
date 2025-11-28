package cc.unitmesh.indexer.clustering

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.codegraph.model.ImportInfo
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.indexer.model.ElementType
import cc.unitmesh.indexer.model.SemanticName
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.naming.CommonSuffixRules

/**
 * Clustering strategy based on import dependencies.
 * Uses mpp-codegraph with TreeSitter for accurate AST-based import extraction.
 * 
 * 基于 Import 依赖的聚类策略：
 * 1. 使用 TreeSitter 解析 AST 提取 import 语句
 * 2. 构建依赖图
 * 3. 使用连通分量算法聚类
 */
class ImportDependencyClusterer(
    private val codeParser: CodeParser? = null
) : ClusteringStrategy {
    
    private val logger = getLogger("ImportDependencyClusterer")
    private val suffixRules = CommonSuffixRules()
    
    override val name: String = "import-dependency"
    
    override suspend fun cluster(
        files: List<String>,
        context: ClusteringContext
    ): List<ModuleCluster> {
        // Use pre-built dependency graph if available, otherwise build from file contents
        val dependencyGraph = context.dependencyGraph 
            ?: buildDependencyGraph(files, context.fileContents)
        
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
     * Build dependency graph from file contents
     * Uses TreeSitter AST parsing when CodeParser is available, fallback to regex otherwise
     */
    private suspend fun buildDependencyGraph(
        files: List<String>,
        fileContents: Map<String, String>
    ): DependencyGraph {
        val builder = DependencyGraphBuilder()
        builder.addNodes(files)
        
        // Group files by language
        val filesByLanguage = files.groupBy { detectLanguage(it) }
        
        for ((language, languageFiles) in filesByLanguage) {
            if (language == Language.UNKNOWN) continue
            
            for (filePath in languageFiles) {
                val content = fileContents[filePath] ?: continue
                
                // Extract imports using TreeSitter (via CodeParser) or fallback to regex
                val imports = if (codeParser != null) {
                    try {
                        codeParser.parseImports(content, filePath, language)
                    } catch (e: Exception) {
                        logger.debug { "TreeSitter parsing failed for $filePath, using regex fallback" }
                        extractImportsFallback(content, language).map { path ->
                            ImportInfo(
                                path = path,
                                type = cc.unitmesh.codegraph.model.ImportType.MODULE,
                                filePath = filePath,
                                startLine = 0,
                                endLine = 0
                            )
                        }
                    }
                } else {
                    // Fallback to regex-based extraction
                    extractImportsFallback(content, language).map { path ->
                        ImportInfo(
                            path = path,
                            type = cc.unitmesh.codegraph.model.ImportType.MODULE,
                            filePath = filePath,
                            startLine = 0,
                            endLine = 0
                        )
                    }
                }
                
                // Resolve imports to actual files in the project
                for (importInfo in imports) {
                    val resolvedFile = resolveImportToFile(importInfo, files, filePath)
                    if (resolvedFile != null && resolvedFile != filePath) {
                        builder.addEdge(filePath, resolvedFile)
                    }
                }
            }
        }
        
        return builder.build()
    }
    
    /**
     * Resolve an ImportInfo to an actual file in the project
     */
    private fun resolveImportToFile(
        importInfo: ImportInfo,
        projectFiles: List<String>,
        currentFile: String
    ): String? {
        // Use ImportInfo's built-in resolution
        for (file in projectFiles) {
            if (file != currentFile && importInfo.couldResolveTo(file)) {
                return file
            }
        }
        
        // Fallback: try pattern-based matching
        val patterns = generateFilePatterns(importInfo.path)
        for (pattern in patterns) {
            val match = projectFiles.find { file ->
                file != currentFile && (file.endsWith(pattern) || file.contains(pattern))
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
            .removeSuffix(".*")
        
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
     * Fallback regex-based import extraction (when CodeParser is not available)
     */
    private fun extractImportsFallback(content: String, language: Language): List<String> {
        return when (language) {
            Language.JAVA, Language.KOTLIN -> extractJvmImportsRegex(content)
            Language.PYTHON -> extractPythonImportsRegex(content)
            Language.JAVASCRIPT, Language.TYPESCRIPT -> extractJsImportsRegex(content)
            Language.GO -> extractGoImportsRegex(content)
            Language.RUST -> extractRustImportsRegex(content)
            else -> emptyList()
        }
    }
    
    private fun extractJvmImportsRegex(content: String): List<String> {
        val importRegex = Regex("""import\s+(static\s+)?([a-zA-Z_][\w.]*[\w*])""")
        return importRegex.findAll(content)
            .map { it.groupValues[2].removeSuffix(".*") }
            .toList()
    }
    
    private fun extractPythonImportsRegex(content: String): List<String> {
        val imports = mutableListOf<String>()
        
        val fromImportRegex = Regex("""from\s+([\w.]+)\s+import""")
        fromImportRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        val importRegex = Regex("""^import\s+([\w.]+)""", RegexOption.MULTILINE)
        importRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        return imports
    }
    
    private fun extractJsImportsRegex(content: String): List<String> {
        val imports = mutableListOf<String>()
        
        val es6ImportRegex = Regex("""import\s+(?:.+\s+from\s+)?['"]([@\w./-]+)['"]""")
        es6ImportRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        val requireRegex = Regex("""require\s*\(\s*['"]([@\w./-]+)['"]\s*\)""")
        requireRegex.findAll(content).forEach { match ->
            imports.add(match.groupValues[1])
        }
        
        return imports
    }
    
    private fun extractGoImportsRegex(content: String): List<String> {
        val imports = mutableListOf<String>()
        val importRegex = Regex("""import\s*\(\s*([\s\S]*?)\s*\)|import\s+"([^"]+)"""")
        
        importRegex.findAll(content).forEach { match ->
            if (match.groupValues[1].isNotEmpty()) {
                val lineRegex = Regex(""""([^"]+)"""")
                lineRegex.findAll(match.groupValues[1]).forEach { lineMatch ->
                    imports.add(lineMatch.groupValues[1])
                }
            } else if (match.groupValues[2].isNotEmpty()) {
                imports.add(match.groupValues[2])
            }
        }
        
        return imports
    }
    
    private fun extractRustImportsRegex(content: String): List<String> {
        val useRegex = Regex("""use\s+([\w:]+)""")
        return useRegex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
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
            
            val component = mutableSetOf<String>()
            val queue = mutableListOf(node)
            
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
        val coreFiles = files
            .sortedByDescending { file ->
                graph.getDependencies(file).count { it in files } +
                graph.getDependents(file).count { it in files }
            }
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
                weight = 0.5f
            )
        }
        
        val cohesion = calculateCohesion(files, graph)
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
    
    private fun inferClusterName(files: List<String>, coreFiles: List<String>): String {
        val commonPrefix = findCommonPathPrefix(files)
        if (commonPrefix.isNotEmpty()) {
            val lastSegment = commonPrefix.split("/").lastOrNull { it.isNotEmpty() }
            if (lastSegment != null && lastSegment.length > 2) {
                return lastSegment.replaceFirstChar { it.uppercase() }
            }
        }
        
        if (coreFiles.isNotEmpty()) {
            val coreFileName = coreFiles.first()
                .substringAfterLast("/")
                .substringBeforeLast(".")
            return suffixRules.normalize(coreFileName).ifEmpty { coreFileName }
        }
        
        return "Module"
    }
    
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
    
    /**
     * Detect programming language from file extension
     */
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
}
