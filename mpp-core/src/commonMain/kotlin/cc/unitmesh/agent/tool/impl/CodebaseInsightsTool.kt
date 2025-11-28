package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.platform.GitOperations
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.model.CodeNode
import cc.unitmesh.codegraph.model.ImportInfo
import cc.unitmesh.codegraph.parser.CodeParser
import cc.unitmesh.codegraph.parser.Language
import cc.unitmesh.devins.filesystem.ProjectFileSystem
import cc.unitmesh.indexer.naming.CamelCaseSplitter
import cc.unitmesh.indexer.naming.CommonSuffixRules
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

/**
 * Schema for CodebaseInsightsTool
 */
object CodebaseInsightsSchema : DeclarativeToolSchema(
    description = "Analyze codebase to extract important domain concepts from Git changes, import dependencies, and code structure",
    properties = mapOf(
        "analysisType" to string(
            description = "Type of analysis: 'full' (Git + Imports + Code), 'git' (Git changes only), 'imports' (Import dependencies only), 'code' (Code structure only)",
            required = false
        ),
        "maxFiles" to integer(
            description = "Maximum number of files to analyze (default: 50)"
        ),
        "focusArea" to string(
            description = "Focus on specific area or module name"
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return "/$toolName analysisType=\"full\" maxFiles=30 focusArea=\"agent\""
    }
}

/**
 * Parameters for CodebaseInsightsTool
 */
@Serializable
data class CodebaseInsightsParams(
    val analysisType: String = "full",
    val maxFiles: Int = 50,
    val focusArea: String? = null
)

/**
 * Result from codebase insights analysis
 */
@Serializable
data class CodebaseInsightsResult(
    val success: Boolean,
    val hotFiles: List<HotFileInfo> = emptyList(),
    val coChangePatterns: Map<String, List<String>> = emptyMap(),
    val domainConcepts: List<DomainConcept> = emptyList(),
    val functionSignatures: Map<String, List<String>> = emptyMap(),
    val statistics: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

/**
 * Information about a frequently changed file
 */
@Serializable
data class HotFileInfo(
    val path: String,
    val changeCount: Int,
    val lastModified: Long = 0,
    val className: String? = null,
    val functions: List<String> = emptyList(),
    val imports: List<String> = emptyList()
)

/**
 * A domain concept extracted from the codebase
 */
@Serializable
data class DomainConcept(
    val name: String,
    val type: String,  // class, interface, function, pattern
    val occurrences: Int,
    val relatedConcepts: List<String> = emptyList(),
    val usageContext: String = "",
    val suggestedTranslation: String = "",
    val description: String = ""
)

/**
 * CodebaseInsightsTool - Analyzes codebase to extract domain concepts
 * 
 * Combines:
 * - Git co-change analysis using GitOperations (frequently changed files)
 * - Import dependency analysis (code structure and relationships)
 * - Code structure analysis (class/function signatures)
 * 
 * Designed to be run asynchronously at agent startup and used
 * for domain dictionary enrichment.
 */
/**
 * Platform-specific factory for creating CodeParser instances
 */
expect fun createCodeParser(): CodeParser

/**
 * Map file extension to Language enum
 */
fun getLanguageFromExtension(extension: String): Language {
    return when (extension.lowercase()) {
        "kt" -> Language.KOTLIN
        "java" -> Language.JAVA
        "py" -> Language.PYTHON
        "js", "jsx" -> Language.JAVASCRIPT
        "ts", "tsx" -> Language.TYPESCRIPT
        "go" -> Language.GO
        "rs" -> Language.RUST
        "cs" -> Language.CSHARP
        else -> Language.UNKNOWN
    }
}

class CodebaseInsightsTool(
    private val fileSystem: ToolFileSystem,
    private val projectPath: String
) {
    private val logger = getLogger("CodebaseInsightsTool")
    private val suffixRules = CommonSuffixRules()
    private val gitOperations = GitOperations(projectPath)
    private val codeParser: CodeParser = createCodeParser()
    
    // ProjectFileSystem adapter for file operations
    private val projectFileSystem: ProjectFileSystem = createProjectFileSystemAdapter(fileSystem, projectPath)
    
    // Cached results for async analysis
    private var cachedResult: CodebaseInsightsResult? = null
    private var analysisJob: Deferred<CodebaseInsightsResult>? = null
    
    val name: String = "codebase-insights"
    
    fun getSchema() = CodebaseInsightsSchema
    
    /**
     * Start async analysis (call at agent startup)
     */
    fun startAsyncAnalysis(
        params: CodebaseInsightsParams = CodebaseInsightsParams(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ): Deferred<CodebaseInsightsResult> {
        logger.info { "Starting async codebase analysis..." }
        
        analysisJob = scope.async {
            try {
                analyze(params)
            } catch (e: Exception) {
                logger.error(e) { "Async analysis failed" }
                CodebaseInsightsResult(
                    success = false,
                    errorMessage = e.message
                )
            }
        }
        
        return analysisJob!!
    }
    
    /**
     * Get cached result or wait for async analysis to complete
     */
    suspend fun getOrAwaitResult(): CodebaseInsightsResult? {
        cachedResult?.let { return it }
        
        return analysisJob?.await()?.also {
            cachedResult = it
        }
    }
    
    /**
     * Check if analysis is complete
     */
    fun isAnalysisComplete(): Boolean {
        return cachedResult != null || (analysisJob?.isCompleted == true)
    }
    
    /**
     * Main analysis logic
     */
    suspend fun analyze(params: CodebaseInsightsParams): CodebaseInsightsResult {
        logger.info { "Starting codebase analysis with params: $params" }
        
        val hotFiles = mutableListOf<HotFileInfo>()
        val coChangePatterns = mutableMapOf<String, List<String>>()
        val domainConcepts = mutableListOf<DomainConcept>()
        val functionSignatures = mutableMapOf<String, List<String>>()
        
        try {
            // Step 1: Analyze Git history using GitOperations
            if (params.analysisType == "full" || params.analysisType == "git") {
                analyzeGitHistory(params, hotFiles, coChangePatterns)
            }
            
            // Step 2: Collect source files and analyze structure
            val sourceFiles = collectSourceFiles(params.maxFiles, params.focusArea)
            
            if (params.analysisType == "full" || params.analysisType == "code") {
                analyzeCodeStructure(sourceFiles, params.focusArea, hotFiles, functionSignatures, domainConcepts)
            }
            
            if (params.analysisType == "full" || params.analysisType == "imports") {
                analyzeImports(sourceFiles, domainConcepts)
            }
            
            // Step 3: Extract domain concepts from all sources
            extractDomainConceptsFromFiles(sourceFiles, domainConcepts)
            
            // Build statistics
            val statistics = buildStatistics(hotFiles, coChangePatterns, domainConcepts)
            
            val result = CodebaseInsightsResult(
                success = true,
                hotFiles = hotFiles.sortedByDescending { it.changeCount }.take(params.maxFiles),
                coChangePatterns = coChangePatterns,
                domainConcepts = domainConcepts.distinctBy { it.name.lowercase() }
                    .sortedByDescending { it.occurrences },
                functionSignatures = functionSignatures,
                statistics = statistics
            )
            
            cachedResult = result
            return result
            
        } catch (e: Exception) {
            logger.error(e) { "Codebase analysis failed" }
            return CodebaseInsightsResult(
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Analyze Git history using GitOperations
     */
    private suspend fun analyzeGitHistory(
        params: CodebaseInsightsParams,
        hotFiles: MutableList<HotFileInfo>,
        coChangePatterns: MutableMap<String, List<String>>
    ) {
        if (!gitOperations.isSupported()) {
            logger.warn { "Git operations not supported on this platform" }
            return
        }
        
        try {
            // Get recent commits
            val commits = gitOperations.getRecentCommits(100)
            logger.debug { "Retrieved ${commits.size} commits for analysis" }
            
            // Build file change frequency map
            val fileChangeCount = mutableMapOf<String, Int>()
            
            for (commit in commits) {
                // Get files from commit diff
                val commitDiff = gitOperations.getCommitDiff(commit.hash)
                val diffFiles = commitDiff?.files?.map { it.path } ?: emptyList()
                
                // Count file changes
                for (file in diffFiles) {
                    // Filter by focus area if specified
                    if (params.focusArea != null && !file.contains(params.focusArea, ignoreCase = true)) {
                        continue
                    }
                    
                    // Only count source files
                    if (isSourceFile(file)) {
                        fileChangeCount[file] = (fileChangeCount[file] ?: 0) + 1
                    }
                }
                
                // Build co-change patterns (files changed together)
                if (diffFiles.size in 2..10) {  // Reasonable co-change size
                    for (file in diffFiles) {
                        if (isSourceFile(file)) {
                            val coChanged = diffFiles.filter { it != file && isSourceFile(it) }
                            if (coChanged.isNotEmpty()) {
                                val existing = coChangePatterns[file] ?: emptyList()
                                coChangePatterns[file] = (existing + coChanged).distinct()
                            }
                        }
                    }
                }
            }
            
            // Create hot files from change frequency
            for ((file, count) in fileChangeCount.entries.sortedByDescending { it.value }) {
                if (count >= 2) {  // At least 2 changes to be considered "hot"
                    hotFiles.add(HotFileInfo(
                        path = file,
                        changeCount = count,
                        className = extractClassName(file)
                    ))
                }
            }
            
            logger.info { "Found ${hotFiles.size} hot files and ${coChangePatterns.size} co-change patterns" }
            
        } catch (e: Exception) {
            logger.warn { "Git history analysis failed: ${e.message}" }
        }
    }
    
    /**
     * Collect source files from project using ProjectFileSystem
     */
    private fun collectSourceFiles(maxFiles: Int, focusArea: String?): List<String> {
        val pattern = focusArea?.let { "*$it*" } ?: "*"
        
        return projectFileSystem.searchFiles(pattern, maxDepth = 10, maxResults = maxFiles * 2)
            .filter { file ->
                // Exclude test files and generated code
                !file.contains("/test/") &&
                !file.contains("/tests/") &&
                !file.contains("Test.") &&
                !file.contains("Spec.") &&
                !file.contains("/generated/") &&
                !file.contains("/build/")
            }
            .distinct()
            .take(maxFiles)
    }
    
    /**
     * Analyze code structure to extract class/function signatures
     */
    private suspend fun analyzeCodeStructure(
        files: List<String>,
        focusArea: String?,
        hotFiles: MutableList<HotFileInfo>,
        functionSignatures: MutableMap<String, List<String>>,
        domainConcepts: MutableList<DomainConcept>
    ) {
        for (filePath in files) {
            try {
                val content = fileSystem.readFile(filePath) ?: continue
                val signatures = extractSignatures(content, filePath)
                
                if (signatures.isNotEmpty()) {
                    functionSignatures[filePath] = signatures
                    
                    // Update or create hot file info
                    val existingHotFile = hotFiles.find { it.path == filePath }
                    if (existingHotFile != null) {
                        val index = hotFiles.indexOf(existingHotFile)
                        hotFiles[index] = existingHotFile.copy(
                            functions = signatures,
                            className = extractClassName(filePath)
                        )
                    } else {
                        hotFiles.add(HotFileInfo(
                            path = filePath,
                            changeCount = 1,
                            className = extractClassName(filePath),
                            functions = signatures
                        ))
                    }
                    
                    // Extract concepts from CodeNodes directly
                    val extension = filePath.substringAfterLast(".").lowercase()
                    val language = getLanguageFromExtension(extension)
                    
                    if (language != Language.UNKNOWN) {
                        try {
                            val nodes = codeParser.parseNodes(content, filePath, language)
                            for (node in nodes) {
                                val conceptName = suffixRules.normalize(node.name)
                                if (conceptName.length > 2) {
                                    val conceptType = when (node.type) {
                                        CodeElementType.CLASS, CodeElementType.INTERFACE -> "class"
                                        CodeElementType.FUNCTION, CodeElementType.METHOD -> "function"
                                        else -> "code-element"
                                    }
                                    addOrIncrementConcept(
                                        domainConcepts,
                                        conceptName,
                                        conceptType,
                                        1,
                                        "${node.type.name.lowercase()} ${node.name}"
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // Continue with other files
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue with other files
            }
        }
    }
    
    /**
     * Analyze imports to find dependencies
     */
    private suspend fun analyzeImports(
        files: List<String>,
        domainConcepts: MutableList<DomainConcept>
    ) {
        for (filePath in files) {
            try {
                val content = fileSystem.readFile(filePath) ?: continue
                val imports = extractImports(content, filePath)
                
                // Extract concepts from imports using rich ImportInfo metadata
                for (importInfo in imports) {
                    // Extract from main import path
                    val lastPart = importInfo.getSimpleName()
                    if (lastPart.length > 2 && !lastPart.all { it.isLowerCase() }) {
                        addOrIncrementConcept(
                            domainConcepts, 
                            lastPart, 
                            "import", 
                            1, 
                            "Import: ${importInfo.path}"
                        )
                    }
                    
                    // Extract from imported names (for selective imports)
                    for (importedName in importInfo.importedNames) {
                        if (importedName.length > 2 && !importedName.all { it.isLowerCase() }) {
                            addOrIncrementConcept(
                                domainConcepts, 
                                importedName, 
                                "import", 
                                1, 
                                "Import: ${importInfo.path}.${importedName}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue with other files
            }
        }
    }
    
    /**
     * Extract domain concepts from file names and structure
     */
    private fun extractDomainConceptsFromFiles(
        files: List<String>,
        domainConcepts: MutableList<DomainConcept>
    ) {
        for (filePath in files) {
            val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
            val normalized = suffixRules.normalize(fileName)
            
            if (normalized.length > 2) {
                val words = CamelCaseSplitter.splitAndFilter(normalized, suffixRules)
                for (word in words.filter { it.length > 2 }) {
                    addOrIncrementConcept(domainConcepts, word, "file-term", 1, "File: $fileName")
                }
            }
        }
    }
    
    /**
     * Extract imports from file content using TreeSitter AST parsing
     */
    private suspend fun extractImports(content: String, filePath: String): List<ImportInfo> {
        val extension = filePath.substringAfterLast(".").lowercase()
        val language = getLanguageFromExtension(extension)
        
        // Return empty list for unsupported languages
        if (language == Language.UNKNOWN) {
            return emptyList()
        }
        
        return try {
            codeParser.parseImports(content, filePath, language)
        } catch (e: Exception) {
            logger.warn { "Failed to parse imports for $filePath: ${e.message}" }
            emptyList()
        }
    }
    
    /**
     * Extract class/function signatures using TreeSitter AST parsing
     */
    private suspend fun extractSignatures(content: String, filePath: String): List<String> {
        val extension = filePath.substringAfterLast(".").lowercase()
        val language = getLanguageFromExtension(extension)
        
        if (language == Language.UNKNOWN) {
            return emptyList()
        }
        
        return try {
            val nodes = codeParser.parseNodes(content, filePath, language)
            nodes.mapNotNull { node ->
                when (node.type) {
                    CodeElementType.CLASS -> "class ${node.name}"
                    CodeElementType.INTERFACE -> "interface ${node.name}"
                    CodeElementType.FUNCTION, CodeElementType.METHOD -> "fun ${node.name}"
                    CodeElementType.CONSTRUCTOR -> "constructor ${node.name}"
                    else -> null
                }
            }.take(30)
        } catch (e: Exception) {
            logger.warn { "Failed to extract signatures for $filePath: ${e.message}" }
            emptyList()
        }
    }
    
    private fun addOrIncrementConcept(
        concepts: MutableList<DomainConcept>,
        name: String,
        type: String,
        count: Int,
        context: String
    ) {
        val existing = concepts.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            val index = concepts.indexOf(existing)
            concepts[index] = existing.copy(
                occurrences = existing.occurrences + count,
                usageContext = if (existing.usageContext.length < 200) {
                    "${existing.usageContext}; $context"
                } else existing.usageContext
            )
        } else {
            concepts.add(DomainConcept(
                name = name,
                type = type,
                occurrences = count,
                usageContext = context
            ))
        }
    }
    
    private fun extractClassName(filePath: String): String? {
        val fileName = filePath.substringAfterLast("/").substringBeforeLast(".")
        return if (fileName.isNotEmpty()) fileName else null
    }
    
    private fun isSourceFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast(".").lowercase()
        return extension in setOf("kt", "java", "py", "ts", "tsx", "js", "jsx", "go", "rs", "cs")
    }
    
    private fun buildStatistics(
        hotFiles: List<HotFileInfo>,
        coChangePatterns: Map<String, List<String>>,
        concepts: List<DomainConcept>
    ): Map<String, String> {
        return mapOf(
            "hotFilesCount" to hotFiles.size.toString(),
            "coChangePatternsCount" to coChangePatterns.size.toString(),
            "conceptsCount" to concepts.size.toString(),
            "topConcepts" to concepts.take(10).joinToString(", ") { "${it.name}(${it.occurrences})" }
        )
    }
    
    /**
     * Get domain concepts for enriching domain dictionary
     */
    fun getDomainConceptsForDictionary(): List<DomainConcept> {
        return cachedResult?.domainConcepts?.filter { it.occurrences >= 2 } ?: emptyList()
    }
    
    /**
     * Get function signatures for a specific file
     */
    fun getFunctionSignatures(filePath: String): List<String> {
        return cachedResult?.functionSignatures?.get(filePath) ?: emptyList()
    }
    
    /**
     * Reset cached results
     */
    fun reset() {
        cachedResult = null
        analysisJob = null
    }
}

/**
 * Create ProjectFileSystem adapter from ToolFileSystem
 */
private fun createProjectFileSystemAdapter(toolFS: ToolFileSystem, projectPath: String): ProjectFileSystem {
    return object : ProjectFileSystem {
        override fun getProjectPath() = projectPath
        
        // Note: readFile is not used in this adapter since we use fileSystem.readFile directly
        override fun readFile(path: String): String? = null
        
        override fun readFileAsBytes(path: String): ByteArray? = null  // Not supported by ToolFileSystem
        
        override fun writeFile(path: String, content: String) = false  // Not needed for insights
        
        override fun exists(path: String) = toolFS.exists(path)
        
        override fun isDirectory(path: String) = toolFS.getFileInfo(path)?.isDirectory ?: false
        
        override fun listFiles(path: String, pattern: String?): List<String> {
            return toolFS.listFiles(path)
        }
        
        override fun searchFiles(pattern: String, maxDepth: Int, maxResults: Int): List<String> {
            val results = mutableListOf<String>()
            val extensions = setOf("kt", "java", "py", "ts", "tsx", "js", "jsx", "go", "rs", "cs")
            
            fun searchRecursive(dir: String, depth: Int) {
                if (depth > maxDepth || results.size >= maxResults) return
                
                try {
                    val items = toolFS.listFiles(dir)
                    for (item in items) {
                        if (results.size >= maxResults) break
                        
                        val fileInfo = toolFS.getFileInfo(item)
                        if (fileInfo?.isDirectory == true) {
                            val dirName = item.substringAfterLast("/")
                            if (dirName !in setOf("node_modules", ".git", "build", "target", ".gradle", "dist", "kcef-cache")) {
                                searchRecursive(item, depth + 1)
                            }
                        } else {
                            val ext = item.substringAfterLast(".").lowercase()
                            if (ext in extensions) {
                                val fileName = item.substringAfterLast("/")
                                if (pattern == "*" || fileName.contains(pattern.removeSurrounding("*"), ignoreCase = true)) {
                                    results.add(item)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip directories that can't be read
                }
            }
            
            searchRecursive(projectPath, 0)
            return results
        }
        
        override fun resolvePath(relativePath: String): String {
            return if (relativePath.startsWith("/")) relativePath else "$projectPath/$relativePath"
        }
    }
}
