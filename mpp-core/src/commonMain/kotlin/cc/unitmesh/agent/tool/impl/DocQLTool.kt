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
        
        IMPORTANT: Use 'documentPath' parameter to target specific documents when:
        - Document name matches your query keywords (check available documents list)
        - You want to avoid querying irrelevant documents
        - You have identified relevant documents through keyword matching
        
        If documentPath is not provided, searches all available documents (memory + indexed).
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
                            appendLine("  - ${entity.name}")
                            count++
                        }
                        appendLine()
                    }
                    
                    if (truncated) {
                        appendLine("💡 Tip: Filter by specific files or entity types")
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
    override val name: String = "docql"
    override val description: String = "Executes a DocQL query against available documents (both in-memory and indexed)."
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "DocQL Query",
        tuiEmoji = "📄",
        composeIcon = "description",
        category = ToolCategory.Utility,
        schema = DocQLSchema
    )

    override fun getParameterClass(): String = DocQLParams::class.simpleName ?: "DocQLParams"

    override fun createToolInvocation(params: DocQLParams): ToolInvocation<DocQLParams, ToolResult> {
        return DocQLInvocation(params, this)
    }
}
