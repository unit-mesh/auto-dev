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
    description = "Executes a DocQL query against a registered document. If documentPath is not provided, searches all registered documents.",
    properties = mapOf(
        "query" to string(
            description = "The DocQL query to execute (e.g., '$.content.heading(\"Introduction\")').",
            required = true
        ),
        "documentPath" to string(
            description = "The path of the document to query. If omitted, searches all registered documents.",
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
            ToolResult.Success("No results found in any registered documents.")
        }
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
