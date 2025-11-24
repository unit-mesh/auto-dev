package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.document.DocumentRegistry
import kotlinx.serialization.Serializable

@Serializable
data class DocQLParams(
    val query: String,
    val documentPath: String? = null
)

object DocQLSchema : DeclarativeToolSchema(
    description = """
        Executes a DocQL query against registered documents.
        
        IMPORTANT: Use 'documentPath' parameter to target specific documents when:
        - Document name matches your query keywords (check available documents list)
        - You want to avoid querying irrelevant documents
        - You have identified relevant documents through keyword matching
        
        If documentPath is not provided, searches all registered documents.
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
        "Executing DocQL query: ${params.query} on all registered documents"
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

    private suspend fun querySingleDocument(documentPath: String, query: String): ToolResult {
        val result = DocumentRegistry.queryDocument(documentPath, query)
        if (result != null) {
            return ToolResult.Success(formatDocQLResult(result, documentPath))
        } else {
            return ToolResult.Error("Document not found: $documentPath", ToolErrorType.FILE_NOT_FOUND.code)
        }
    }

    private suspend fun queryAllDocuments(query: String): ToolResult {
        val registeredPaths = DocumentRegistry.getRegisteredPaths()
        if (registeredPaths.isEmpty()) {
            return ToolResult.Error("No documents registered. Please register documents first.", ToolErrorType.FILE_NOT_FOUND.code)
        }

        val results = mutableListOf<String>()
        for (path in registeredPaths) {
            val result = DocumentRegistry.queryDocument(path, query)
            if (result != null && !isEmptyResult(result)) {
                results.add("## Document: $path\n${formatDocQLResult(result, path)}")
            }
        }

        return if (results.isNotEmpty()) {
            ToolResult.Success(results.joinToString("\n\n"))
        } else {
            // Provide helpful suggestions when no results found
            val suggestion = buildQuerySuggestion(query, registeredPaths)
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

    private fun formatDocQLResult(result: cc.unitmesh.devins.document.docql.DocQLResult, documentPath: String): String {
        return when (result) {
            is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> {
                result.items.joinToString("\n") { "${it.level}. ${it.title}" }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> {
                result.items.joinToString("\n") { it.name }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> {
                result.items.joinToString("\n---\n") { it.content }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> {
                result.items.joinToString("\n") { it.code }
            }
            is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> {
                "Tables found: ${result.items.size}"
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
            is cc.unitmesh.devins.document.docql.DocQLResult.TocItems -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.Entities -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.Chunks -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.CodeBlocks -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.Tables -> result.items.isEmpty()
            is cc.unitmesh.devins.document.docql.DocQLResult.Error -> true
        }
    }
}

class DocQLTool : BaseExecutableTool<DocQLParams, ToolResult>() {
    override val name: String = "docql"
    override val description: String = "Executes a DocQL query against a registered document."
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
