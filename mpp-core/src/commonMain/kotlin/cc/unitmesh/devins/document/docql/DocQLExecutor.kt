package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*

/**
 * DocQL query execution result
 */
sealed class DocQLResult {
    /**
     * TOC items result
     */
    data class TocItems(val items: List<TOCItem>) : DocQLResult()
    
    /**
     * Entity items result
     */
    data class Entities(val items: List<Entity>) : DocQLResult()
    
    /**
     * Document chunks result
     */
    data class Chunks(val items: List<DocumentChunk>) : DocQLResult()
    
    /**
     * Code blocks result
     */
    data class CodeBlocks(val items: List<CodeBlock>) : DocQLResult()
    
    /**
     * Tables result
     */
    data class Tables(val items: List<TableBlock>) : DocQLResult()
    
    /**
     * Empty result
     */
    object Empty : DocQLResult()
    
    /**
     * Error result
     */
    data class Error(val message: String) : DocQLResult()
}

/**
 * Code block extracted from document
 */
data class CodeBlock(
    val language: String?,
    val code: String,
    val location: Location
)

/**
 * Table block extracted from document
 */
data class TableBlock(
    val headers: List<String>,
    val rows: List<List<String>>,
    val location: Location
)

/**
 * DocQL Executor - executes DocQL queries against document data
 */
class DocQLExecutor(
    private val documentFile: DocumentFile?,
    private val parserService: DocumentParserService?
) {
    
    /**
     * Execute a DocQL query
     */
    suspend fun execute(query: DocQLQuery): DocQLResult {
        try {
            if (query.nodes.isEmpty()) {
                return DocQLResult.Error("Empty query")
            }
            
            // First node must be Root
            if (query.nodes[0] !is DocQLNode.Root) {
                return DocQLResult.Error("Query must start with $")
            }
            
            // Second node determines the context
            if (query.nodes.size < 2) {
                return DocQLResult.Error("Incomplete query")
            }
            
            return when (val contextNode = query.nodes[1]) {
                is DocQLNode.Property -> {
                    when (contextNode.name) {
                        "toc" -> executeTocQuery(query.nodes.drop(2))
                        "entities" -> executeEntitiesQuery(query.nodes.drop(2))
                        "content" -> executeContentQuery(query.nodes.drop(2))
                        else -> DocQLResult.Error("Unknown context '${contextNode.name}'")
                    }
                }
                else -> DocQLResult.Error("Expected property after $")
            }
        } catch (e: Exception) {
            return DocQLResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Execute TOC query ($.toc[...])
     */
    private fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }
        
        var items = documentFile.toc
        
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all items (flatten tree)
                    items = flattenToc(items)
                }
                
                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }
                
                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter items
                    items = filterTocItems(items, node.condition)
                }
                
                else -> {
                    return DocQLResult.Error("Invalid operation for TOC query")
                }
            }
        }
        
        return DocQLResult.TocItems(items)
    }
    
    /**
     * Execute entities query ($.entities[...])
     */
    private fun executeEntitiesQuery(nodes: List<DocQLNode>): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }
        
        var items = documentFile.entities
        
        for (node in nodes) {
            when (node) {
                is DocQLNode.ArrayAccess.All -> {
                    // Return all entities
                }
                
                is DocQLNode.ArrayAccess.Index -> {
                    // Return specific index
                    items = if (node.index < items.size) {
                        listOf(items[node.index])
                    } else {
                        emptyList()
                    }
                }
                
                is DocQLNode.ArrayAccess.Filter -> {
                    // Filter entities
                    items = filterEntities(items, node.condition)
                }
                
                else -> {
                    return DocQLResult.Error("Invalid operation for entities query")
                }
            }
        }
        
        return DocQLResult.Entities(items)
    }
    
    /**
     * Execute content query ($.content.heading(...), $.content.h1(...), etc.)
     */
    private suspend fun executeContentQuery(nodes: List<DocQLNode>): DocQLResult {
        if (nodes.isEmpty()) {
            return DocQLResult.Error("Content query requires function call (e.g., $.content.chunks() or $.content.heading(\"keyword\"))")
        }
        
        val functionNode = nodes.firstOrNull { it is DocQLNode.FunctionCall } as? DocQLNode.FunctionCall
            ?: return DocQLResult.Error("Content query requires function call (e.g., $.content.chunks() or $.content.heading(\"keyword\"))")
        
        return when (functionNode.name) {
            "heading" -> executeHeadingQuery(functionNode.argument)
            "chapter" -> executeChapterQuery(functionNode.argument)
            "h1", "h2", "h3", "h4", "h5", "h6" -> executeHeadingLevelQuery(functionNode.name, functionNode.argument)
            "grep" -> executeGrepQuery(functionNode.argument)
            "code" -> executeCodeQuery(nodes)
            "table" -> executeTableQuery(nodes)
            "chunks" -> executeAllChunksQuery()
            "all" -> executeAllChunksQuery()
            else -> DocQLResult.Error("Unknown content function '${functionNode.name}'")
        }
    }
    
    /**
     * Execute all chunks query: $.content.chunks() or $.content.all()
     */
    private suspend fun executeAllChunksQuery(): DocQLResult {
        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }
        
        // Query all headings with empty keyword to get all content chunks
        // This is a workaround since there's no direct "get all chunks" method
        val allHeadings = documentFile?.toc?.let { flattenToc(it) } ?: emptyList()
        
        if (allHeadings.isEmpty()) {
            return DocQLResult.Empty
        }
        
        // Query each heading to get its content
        val chunks = mutableListOf<DocumentChunk>()
        for (heading in allHeadings) {
            val chunkContent = parserService.queryChapter(heading.anchor.removePrefix("#"))
            if (chunkContent != null) {
                chunks.add(chunkContent)
            }
        }
        
        return if (chunks.isEmpty()) {
            DocQLResult.Empty
        } else {
            DocQLResult.Chunks(chunks)
        }
    }
    
    /**
     * Execute heading query: $.content.heading("keyword")
     */
    private suspend fun executeHeadingQuery(keyword: String): DocQLResult {
        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }
        
        val chunks = parserService.queryHeading(keyword)
        return if (chunks.isEmpty()) {
            // Try partial matching with more flexible search by querying TOC
            val allHeadings = documentFile?.toc?.let { flattenToc(it) } ?: emptyList()
            val partialMatches = mutableListOf<DocumentChunk>()
            
            for (heading in allHeadings) {
                if (heading.title.contains(keyword, ignoreCase = true)) {
                    val chunk = parserService.queryChapter(heading.anchor.removePrefix("#"))
                    if (chunk != null) {
                        partialMatches.add(chunk)
                    }
                }
            }
            
            if (partialMatches.isNotEmpty()) {
                DocQLResult.Chunks(partialMatches)
            } else {
                DocQLResult.Empty
            }
        } else {
            DocQLResult.Chunks(chunks)
        }
    }
    
    /**
     * Execute chapter query: $.content.chapter("id")
     */
    private suspend fun executeChapterQuery(id: String): DocQLResult {
        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }
        
        val chunk = parserService.queryChapter(id)
        return if (chunk != null) {
            DocQLResult.Chunks(listOf(chunk))
        } else {
            DocQLResult.Empty
        }
    }
    
    /**
     * Execute heading level query: $.content.h1("keyword")
     */
    private fun executeHeadingLevelQuery(level: String, keyword: String): DocQLResult {
        if (documentFile == null) {
            return DocQLResult.Error("No document loaded")
        }
        
        val levelNum = level.substring(1).toIntOrNull() ?: return DocQLResult.Error("Invalid heading level")
        
        val matchingTocs = flattenToc(documentFile.toc).filter {
            it.level == levelNum && it.title.contains(keyword, ignoreCase = true)
        }
        
        return DocQLResult.TocItems(matchingTocs)
    }
    
    /**
     * Execute grep query: $.content.grep("pattern")
     */
    private suspend fun executeGrepQuery(pattern: String): DocQLResult {
        if (parserService == null) {
            return DocQLResult.Error("No parser service available")
        }
        
        // Use heading query with pattern as a simple implementation
        // In a real implementation, this would do full-text search
        val chunks = parserService.queryHeading(pattern)
        return DocQLResult.Chunks(chunks)
    }
    
    /**
     * Execute code query: $.content.code[*]
     */
    private fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
        // TODO: Implement code block extraction
        return DocQLResult.CodeBlocks(emptyList())
    }
    
    /**
     * Execute table query: $.content.table[*]
     */
    private fun executeTableQuery(nodes: List<DocQLNode>): DocQLResult {
        // TODO: Implement table extraction
        return DocQLResult.Tables(emptyList())
    }
    
    /**
     * Flatten TOC tree to list
     */
    private fun flattenToc(items: List<TOCItem>): List<TOCItem> {
        val result = mutableListOf<TOCItem>()
        for (item in items) {
            result.add(item)
            result.addAll(flattenToc(item.children))
        }
        return result
    }
    
    /**
     * Filter TOC items by condition
     */
    private fun filterTocItems(items: List<TOCItem>, condition: FilterCondition): List<TOCItem> {
        val flattened = flattenToc(items)
        return flattened.filter { item ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "level" -> item.level.toString() == condition.value
                        "title" -> item.title == condition.value
                        else -> false
                    }
                }
                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "title" -> item.title.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }
                is FilterCondition.GreaterThan -> {
                    when (condition.property) {
                        "level" -> item.level > condition.value
                        "page" -> (item.page ?: 0) > condition.value
                        else -> false
                    }
                }
                is FilterCondition.LessThan -> {
                    when (condition.property) {
                        "level" -> item.level < condition.value
                        "page" -> (item.page ?: 0) < condition.value
                        else -> false
                    }
                }
            }
        }
    }
    
    /**
     * Filter entities by condition
     */
    private fun filterEntities(items: List<Entity>, condition: FilterCondition): List<Entity> {
        return items.filter { entity ->
            when (condition) {
                is FilterCondition.Equals -> {
                    when (condition.property) {
                        "name" -> entity.name == condition.value
                        "type" -> {
                            val entityType = when (entity) {
                                is Entity.Term -> "Term"
                                is Entity.API -> "API"
                                is Entity.ClassEntity -> "ClassEntity"
                                is Entity.FunctionEntity -> "FunctionEntity"
                            }
                            entityType == condition.value
                        }
                        else -> false
                    }
                }
                is FilterCondition.Contains -> {
                    when (condition.property) {
                        "name" -> entity.name.contains(condition.value, ignoreCase = true)
                        else -> false
                    }
                }
                else -> false
            }
        }
    }
}

/**
 * Convenience function to parse and execute DocQL query
 */
suspend fun executeDocQL(
    queryString: String,
    documentFile: DocumentFile?,
    parserService: DocumentParserService?
): DocQLResult {
    return try {
        val query = parseDocQL(queryString)
        val executor = DocQLExecutor(documentFile, parserService)
        executor.execute(query)
    } catch (e: DocQLException) {
        DocQLResult.Error(e.message ?: "Parse error")
    } catch (e: Exception) {
        DocQLResult.Error(e.message ?: "Execution error")
    }
}

