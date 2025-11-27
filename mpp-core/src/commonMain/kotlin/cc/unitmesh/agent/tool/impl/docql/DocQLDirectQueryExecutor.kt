package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.docql.DocQLResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Executor for direct DocQL queries (non-smart search).
 */
class DocQLDirectQueryExecutor(private val maxResults: Int) {

    suspend fun querySingleDocument(documentPath: String?, query: String): ToolResult {
        if (documentPath == null || documentPath == "null") {
            return queryAllDocuments(query)
        }

        val result = DocumentRegistry.queryDocument(documentPath, query)
        if (result == null) {
            logger.info { "Document '$documentPath' not found, falling back to global search" }
            return queryAllDocuments(query)
        }

        val stats = DocQLSearchStats(
            searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
            channels = listOf(extractQueryType(query)),
            documentsSearched = 1,
            totalRawResults = result.getResultCount(),
            resultsAfterRerank = result.getResultCount().coerceAtMost(maxResults),
            truncated = result.getResultCount() > maxResults,
            usedFallback = false,
            rerankerConfig = null, // No reranker for direct queries
            scoringInfo = null
        )

        return ToolResult.Success(
            result.formatDocQLResult(maxResults),
            stats.toMetadata()
        )
    }

    suspend fun queryAllDocuments(query: String): ToolResult {
        val result = DocumentRegistry.queryDocuments(query)

        return if (!result.isEmptyResult()) {
            val docsSearched = when (result) {
                is DocQLResult.Entities -> result.itemsByFile.size
                is DocQLResult.TocItems -> result.itemsByFile.size
                is DocQLResult.Chunks -> result.itemsByFile.size
                is DocQLResult.CodeBlocks -> result.itemsByFile.size
                is DocQLResult.Tables -> result.itemsByFile.size
                is DocQLResult.Files -> result.items.size
                else -> 0
            }

            val stats = DocQLSearchStats(
                searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
                channels = listOf(extractQueryType(query)),
                documentsSearched = docsSearched,
                totalRawResults = result.getResultCount(),
                resultsAfterRerank = result.getResultCount().coerceAtMost(maxResults),
                truncated = result.getResultCount() > maxResults,
                usedFallback = false,
                rerankerConfig = null,
                scoringInfo = null
            )

            ToolResult.Success(
                result.formatDocQLResult(maxResults),
                stats.toMetadata()
            )
        } else {
            // Provide helpful suggestions when no results found
            val availablePaths = DocumentRegistry.getRootRegisteredPaths()
            if (availablePaths.isEmpty()) {
                return ToolResult.Error(
                    "No documents available. Please register or index documents first.",
                    ToolErrorType.FILE_NOT_FOUND.code
                )
            }
            val suggestion = DocQLResultFormatter.buildQuerySuggestion(query)

            val emptyStats = DocQLSearchStats(
                searchType = DocQLSearchStats.SearchType.DIRECT_QUERY,
                channels = listOf(extractQueryType(query)),
                documentsSearched = 0,
                totalRawResults = 0,
                resultsAfterRerank = 0,
                truncated = false,
                usedFallback = false,
                rerankerConfig = null,
                scoringInfo = null
            )

            ToolResult.Success(
                "No results found for query: $query\n\n$suggestion",
                emptyStats.toMetadata()
            )
        }
    }

    private fun extractQueryType(query: String): String {
        return when {
            query.contains("$.code.class") -> "class"
            query.contains("$.code.function") -> "function"
            query.contains("$.code.classes") -> "classes"
            query.contains("$.code.functions") -> "functions"
            query.contains("$.content.heading") -> "heading"
            query.contains("$.content.chunks") -> "chunks"
            query.contains("$.toc") -> "toc"
            query.contains("$.files") -> "files"
            else -> "unknown"
        }
    }
}
