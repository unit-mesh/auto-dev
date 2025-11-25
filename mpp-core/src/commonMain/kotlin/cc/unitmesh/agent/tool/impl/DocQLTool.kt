package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.integer
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.document.DocumentRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

@Serializable
data class DocQLParams(
    val query: String,
    val documentPath: String? = null,
    val maxResults: Int? = 20
)

object DocQLSchema : DeclarativeToolSchema(
    description = """
        Executes a DocQL query against available documents (both in-memory and indexed).
        
        ## TWO QUERY SYSTEMS
        
        ### 1. Document Queries ($.content.*, $.toc[*])
        **For:** Markdown, text files, documentation (.md, .txt, README)
        **Examples:**
        - $.content.heading("keyword") - Find sections by heading
        - $.content.chunks() - Get all content chunks
        - $.toc[*] - Get table of contents
        
        ### 2. Code Queries ($.code.*)
        **For:** Source code files (.kt, .java, .py, .js, .ts, .go, .rs, .cs)
        **Parser:** TreeSitter-based with full code structure
        **Examples:**
        - $.code.class("ClassName") - Find class with full source code
        - $.code.function("functionName") - Find function/method with implementation
        - $.code.classes[*] - List all classes
        - $.code.functions[*] - List all functions/methods
        - $.code.classes[?(@.name contains "Parser")] - Filter by name pattern
        
        ## CRITICAL: For Code Questions
        **ALWAYS use BOTH query systems and compare results:**
        1. Try $.code.class("X") or $.code.function("X") (TreeSitter-optimized)
        2. Also try $.content.heading("X") (fallback)
        3. Use the result with better content
        
        ## Parameters
        - **query** (required): The DocQL query string
        - **documentPath** (optional): Target specific document by path
        - **maxResults** (optional): Limit results (default: 20)
        
        ## Usage Tips
        - For "How does X work?": Try $.code.class("X") + $.content.heading("X")
        - For "Find all Y methods": Try $.code.functions[?(@.name contains "Y")]
        - For documentation: Use $.content.heading() or $.content.chunks()
        - Always query both systems for code to get best results
    """.trimIndent(),
    properties = mapOf(
        "query" to string(
            description = "The DocQL query to execute (e.g., '$.content.heading(\"Introduction\")').",
            required = true
        ),
        "documentPath" to string(
            description = """
                The path of the document to query (e.g., 'design-system-color.md').
                Use this to target specific documents when their names match your keywords.
                Check the available documents list and match keywords before querying.
                If omitted, searches all registered documents.
            """.trimIndent(),
            required = false
        ),
        "maxResults" to integer(
            description = """
                Maximum number of results to return. Default is 20.
                Use lower values for quick overview, higher values for comprehensive search.
                Note: Very high values may exceed context limits for large result sets.
            """.trimIndent(),
            required = false
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """
            /$toolName
            ```json
            {"query": "$.content.heading(\"Introduction\")"}
            ```
            
            Or with specific document:
            /$toolName
            ```json
            {"query": "$.content.heading(\"Introduction\")", "documentPath": "path/to/doc.md"}
            ```
        """.trimIndent()
    }
}

class DocQLInvocation(
    params: DocQLParams,
    tool: DocQLTool
) : BaseToolInvocation<DocQLParams, ToolResult>(params, tool) {
    override fun getDescription(): String = if (params.documentPath != null) {
        "Executing DocQL query: ${params.query} on ${params.documentPath}"
    } else {
        "Executing DocQL query: ${params.query} on all available documents"
    }

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.COMMAND_FAILED) {
            if (params.documentPath != null) {
                // Query specific document
                querySingleDocument(params.documentPath, params.query)
            } else {
                // Query all registered documents
                queryAllDocuments(params.query)
            }
        }
    }

    private suspend fun querySingleDocument(documentPath: String?, query: String): ToolResult {
        // If documentPath is null or "null", delegate to global search
        if (documentPath == null || documentPath == "null") {
            return queryAllDocuments(query)
        }
        
        val result = DocumentRegistry.queryDocument(documentPath, query)
        
        // If document not found, fall back to global search
        if (result == null) {
            logger.info { "Document '$documentPath' not found, falling back to global search" }
            return queryAllDocuments(query)
        }
        
        return ToolResult.Success(formatDocQLResult(result, documentPath, params.maxResults ?: 20))
    }

    private suspend fun queryAllDocuments(query: String): ToolResult {
        // Use the new multi-file query API
        val result = DocumentRegistry.queryDocuments(query)
        
        return if (!isEmptyResult(result)) {
            ToolResult.Success(formatDocQLResult(result, null, params.maxResults ?: 20))
        } else {
            // Provide helpful suggestions when no results found
            val availablePaths = DocumentRegistry.getAllAvailablePaths()
            if (availablePaths.isEmpty()) {
                return ToolResult.Error(
                    "No documents available. Please register or index documents first.", 
                    ToolErrorType.FILE_NOT_FOUND.code
                )
            }
            val suggestion = buildQuerySuggestion(query, availablePaths)
            ToolResult.Success("No results found for query: $query\n\n$suggestion")
        }
    }

    private fun buildQuerySuggestion(query: String, registeredPaths: List<String>): String {
        val suggestions = mutableListOf<String>()
        
        suggestions.add("💡 **Suggestions to find the information:**")
        
        // Suggest checking TOC if not already a TOC query
        if (!query.contains("toc")) {
            suggestions.add("1. Try `$.toc[*]` to see all available sections in the documents")
        }
        
        // Suggest broader search if query looks specific
        if (query.contains("heading") || query.contains("h1") || query.contains("h2")) {
            suggestions.add("2. Try a broader heading search with fewer keywords")
            suggestions.add("3. Try `$.content.chunks()` to get all content and search manually")
        } else if (!query.contains("chunks")) {
            suggestions.add("2. Try `$.content.chunks()` to retrieve all document content")
        }
        
        // List available documents
        if (registeredPaths.isNotEmpty()) {
            suggestions.add("\n📚 **Available documents:**")
            registeredPaths.forEach { path ->
                suggestions.add("   - $path")
            }
        }
        
        return suggestions.joinToString("\n")
    }

    private fun formatDocQLResult(
        result: cc.unitmesh.devins.document.docql.DocQLResult, 
        documentPath: String?,
        maxResults: Int = 20
    ): String {
        return when (result) {
            is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems TOC items across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        appendLine("## 📄 $filePath")
                        for (item in items) {
                            if (count >= maxResults) break
                            appendLine("  ${"  ".repeat(item.level - 1)}${item.level}. ${item.title}")
                            count++
                        }
                        appendLine()
                    }
                    
                    if (truncated) {
                        appendLine("💡 Tip: Query specific directories to get more focused results:")
                        appendLine("   \$.toc[?(@.title contains \"keyword\")]")
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems entities across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        appendLine("## 📄 $filePath")
                        for (entity in items) {
                            if (count >= maxResults) break
                            when (entity) {
                                is cc.unitmesh.devins.document.Entity.ClassEntity -> {
                                    val pkg = if (!entity.packageName.isNullOrEmpty()) " (${entity.packageName})" else ""
                                    appendLine("  📘 class ${entity.name}$pkg")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }
                                is cc.unitmesh.devins.document.Entity.FunctionEntity -> {
                                    val sig = entity.signature ?: entity.name
                                    appendLine("  ⚡ $sig")
                                    if (entity.location.line != null) {
                                        appendLine("     └─ Line ${entity.location.line}")
                                    }
                                }
                                is cc.unitmesh.devins.document.Entity.Term -> {
                                    appendLine("  📝 ${entity.name}: ${entity.definition ?: ""}")
                                }
                                is cc.unitmesh.devins.document.Entity.API -> {
                                    appendLine("  🔌 ${entity.name}: ${entity.signature ?: ""}")
                                }
                            }
                            count++
                        }
                        appendLine()
                    }
                    
                    if (truncated) {
                        appendLine("💡 Tip: Use $.code.class(\"ClassName\") or $.code.function(\"functionName\") to get full source code")
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> {
                buildString {
                    val totalItems = result.totalCount
                    val truncated = totalItems > maxResults
                    
                    appendLine("Found $totalItems content chunks across ${result.itemsByFile.size} file(s):")
                    if (truncated) {
                        appendLine("⚠️ Showing first $maxResults results (${totalItems - maxResults} more available)")
                        appendLine("💡 Tip: Narrow down your search to specific files or directories")
                        appendLine("   Example: Query documents in a specific directory only")
                    }
                    appendLine()
                    
                    var count = 0
                    for ((filePath, items) in result.itemsByFile) {
                        if (count >= maxResults) break
                        
                        // Filter out empty or whitespace-only chunks
                        val nonEmptyItems = items.filter { it.content.trim().isNotEmpty() }
                        if (nonEmptyItems.isEmpty()) continue
                        
                        appendLine("## 📄 $filePath")
                        appendLine()
                        
                        for (chunk in nonEmptyItems) {
                            if (count >= maxResults) break
                            
                            val content = chunk.content.trim()
                            if (content.isNotEmpty()) {
                                appendLine(content)
                                appendLine()
                                appendLine("---")
                                appendLine()
                                count++
                            }
                        }
                    }
                    
                    if (count == 0) {
                        appendLine("No non-empty chunks found.")
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> {
                buildString {
                    appendLine("Found ${result.totalCount} code blocks across ${result.itemsByFile.size} file(s):")
                    appendLine()
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## 📄 $filePath")
                        items.forEach { block ->
                            appendLine("```${block.language ?: ""}")
                            appendLine(block.code)
                            appendLine("```")
                            appendLine()
                        }
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> {
                buildString {
                    appendLine("Found ${result.totalCount} tables across ${result.itemsByFile.size} file(s):")
                    for ((filePath, items) in result.itemsByFile) {
                        appendLine("## 📄 $filePath")
                        appendLine("  ${items.size} table(s)")
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Files -> {
                buildString {
                    appendLine("Found ${result.items.size} files:")
                    appendLine()
                    result.items.forEach { file ->
                        appendLine("📄 ${file.path}")
                        if (file.directory.isNotEmpty()) {
                            appendLine("   Directory: ${file.directory}")
                        }
                        if (file.extension.isNotEmpty()) {
                            appendLine("   Type: ${file.extension}")
                        }
                        if (file.size > 0) {
                            appendLine("   Size: ${file.size} characters")
                        }
                        appendLine()
                    }
                    if (result.items.size > 50) {
                        appendLine("💡 Too many results! Consider filtering by directory:")
                        appendLine("   \$.files[?(@.path contains \"your-directory\")]")
                    }
                }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Empty -> {
                "No results found."
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Error -> {
                throw ToolException(result.message, ToolErrorType.COMMAND_FAILED)
            }
        }
    }

    private fun isEmptyResult(result: cc.unitmesh.devins.document.docql.DocQLResult): Boolean {
        return when (result) {
            is cc.unitmesh.devins.document.docql.DocQLResult.Empty -> true
            is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> result.totalCount == 0
            is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> result.totalCount == 0
            is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> result.totalCount == 0
            is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> result.totalCount == 0
            is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> result.totalCount == 0
            is cc.unitmesh.devins.document.docql.DocQLResult.Files -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.Error -> true
        }
    }
}

class DocQLTool : BaseExecutableTool<DocQLParams, ToolResult>() {
    override val name: String = "DocQL"
    override val description: String = "Executes a DocQL query against available documents (both in-memory and indexed)."
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "DocQL Query",
        tuiEmoji = "📄",
        composeIcon = "description",
        category = ToolCategory.Utility,
        schema = DocQLSchema
    )

    override fun getParameterClass(): String = DocQLParams::class.simpleName ?: "DocQLParams"

    /**
     * Override createInvocation to handle Map<String, Any> parameters from ToolOrchestrator
     */
    override fun createInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        // Handle both direct DocQLParams and Map<String, Any> from ToolOrchestrator
        val actualParams = when (params) {
            is DocQLParams -> params
            else -> {
                // If params is actually a Map (cast from Any), convert it to DocQLParams
                @Suppress("UNCHECKED_CAST")
                val paramsMap = params as? Map<String, Any> ?: throw ToolException(
                    "Invalid parameters type: expected DocQLParams or Map<String, Any>, got ${params::class.simpleName}",
                    ToolErrorType.INVALID_PARAMETERS
                )
                convertMapToDocQLParams(paramsMap)
            }
        }
        return createToolInvocation(actualParams)
    }

    override fun createToolInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        return DocQLInvocation(params, this)
    }
    
    private fun convertMapToDocQLParams(map: Map<String, Any>): DocQLParams {
        val query = map["query"] as? String
            ?: throw ToolException("Missing required parameter 'query'", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        
        val documentPath = map["documentPath"] as? String
        val maxResults = when (val maxRes = map["maxResults"]) {
            is Int -> maxRes
            is Long -> maxRes.toInt()
            is Double -> maxRes.toInt()
            is String -> maxRes.toIntOrNull()
            null -> null
            else -> null
        }
        
        return DocQLParams(
            query = query,
            documentPath = documentPath,
            maxResults = maxResults ?: 20
        )
    }
}
