package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.devins.document.DocumentFile
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.docql.DocQLResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * Maximum lines to include in function/method preview.
 * This ensures previews are meaningful but not too long.
 */
const val MAX_FUNCTION_PREVIEW_LINES = 20

/**
 * Default character length for previews when code content is not available.
 */
const val DEFAULT_PREVIEW_CHAR_LENGTH = 500

/**
 * Executor for keyword-based search across multiple DocQL channels.
 * 
 * Handles parallel execution of keyword searches across code and content channels,
 * merging and deduplicating results.
 */
class DocQLKeywordSearchExecutor {
    
    /**
     * Execute search for a list of keywords and merge results.
     * 
     * @param keywords List of keywords to search for
     * @param documentPath Optional path to search in a specific document
     * @param reranker Reranker to order merged results
     * @param queryForScoring Query string used for scoring
     * @return Merged and reranked search results
     */
    suspend fun executeKeywordSearch(
        keywords: List<String>,
        documentPath: String?,
        reranker: DocumentReranker,
        queryForScoring: String
    ): SearchLevelResult {
        if (keywords.isEmpty()) {
            return SearchLevelResult(
                items = emptyList(),
                totalCount = 0,
                truncated = false,
                metadata = mutableMapOf(),
                docsSearched = 0,
                activeChannels = emptyList()
            )
        }

        val allItems = mutableListOf<SearchItem>()
        val allMetadata = mutableMapOf<SearchItem, Pair<Any, String?>>()
        var totalDocsSearched = 0
        val allChannels = mutableSetOf<String>()

        // Execute search for each keyword in parallel
        val keywordResults = coroutineScope {
            keywords.map { kw ->
                async {
                    val queryChannels = mapOf(
                        "class" to "$.code.class(\"$kw\")",
                        "function" to "$.code.function(\"$kw\")",
                        "heading" to "$.content.heading(\"$kw\")",
                        "toc" to "$.toc[?(@.title contains \"$kw\")]"
                    )

                    val channelResults = queryChannels.mapNotNull { (channel, query) ->
                        try {
                            val result = if (documentPath != null) {
                                DocumentRegistry.queryDocument(documentPath, query)
                            } else {
                                DocumentRegistry.queryDocuments(query)
                            }
                            if (result != null) channel to result else null
                        } catch (e: Exception) {
                            logger.warn { "Search channel '$channel' for keyword '$kw' failed: ${e.message}" }
                            null
                        }
                    }.toMap()

                    kw to channelResults
                }
            }.awaitAll().toMap()
        }

        // Merge results from all keywords
        keywordResults.forEach { (kw, channelResults) ->
            channelResults.forEach { (channel, result: DocQLResult) ->
                allChannels.add(channel)
                val items = mutableListOf<SearchItem>()
                collectSearchItems(result, items, allMetadata)
                allItems.addAll(items)

                totalDocsSearched += when (result) {
                    is DocQLResult.Entities -> result.itemsByFile.size
                    is DocQLResult.TocItems -> result.itemsByFile.size
                    is DocQLResult.Chunks -> result.itemsByFile.size
                    else -> 0
                }
            }
        }

        // Deduplicate items by their unique ID
        val uniqueItems = allItems.distinctBy { it.segment.id ?: it.hashCode() }

        if (uniqueItems.isEmpty()) {
            return SearchLevelResult(
                items = emptyList(),
                totalCount = 0,
                truncated = false,
                metadata = allMetadata,
                docsSearched = totalDocsSearched,
                activeChannels = allChannels.toList()
            )
        }

        // Rerank the merged results
        val rankedLists = mapOf("merged" to uniqueItems)
        val rerankResult = reranker.rerank(
            rankedLists = rankedLists,
            query = queryForScoring,
            segmentExtractor = { it.segment }
        )

        val rerankedItems = rerankResult.items.map { it.item }

        return SearchLevelResult(
            items = rerankedItems,
            totalCount = rerankResult.totalCount,
            truncated = rerankResult.truncated,
            metadata = allMetadata,
            docsSearched = totalDocsSearched,
            activeChannels = allChannels.toList()
        )
    }

    /**
     * Collect search items from a DocQL result.
     * 
     * Converts various DocQL result types (Entities, TOC items, Chunks) into SearchItems
     * with appropriate metadata.
     * 
     * For Entities (classes, functions), attempts to retrieve actual code content
     * from the document parser to provide meaningful previews.
     */
    fun collectSearchItems(
        result: DocQLResult,
        items: MutableList<SearchItem>,
        itemMetadata: MutableMap<SearchItem, Pair<Any, String?>>
    ) {
        when (result) {
            is DocQLResult.Entities -> {
                result.itemsByFile.forEach { (file, entities) ->
                    // Get the parser for this file to retrieve code content
                    val docPair = DocumentRegistry.getDocument(file)
                    val parser = docPair?.second
                    val docFile = docPair?.first as? DocumentFile
                    val sourceContent = parser?.getDocumentContent()
                    
                    entities.forEach { entity ->
                        val type = when (entity) {
                            is Entity.ClassEntity -> "class"
                            is Entity.FunctionEntity -> "function"
                            is Entity.ConstructorEntity -> "constructor"
                            else -> "entity"
                        }
                        
                        // Try to get actual code content for this entity
                        val codeContent = getEntityCodeContent(entity, sourceContent, docFile)
                        
                        val segment = TextSegment(
                            text = codeContent,
                            metadata = mapOf(
                                "type" to type,
                                "name" to entity.name,
                                "id" to "${file}:${entity.name}:${entity.location.line}",
                                "filePath" to file,
                                "line" to (entity.location.line ?: 0),
                                "signature" to getEntitySignature(entity)
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = entity to file
                    }
                }
            }

            is DocQLResult.TocItems -> {
                result.itemsByFile.forEach { (file, tocItems) ->
                    tocItems.forEach { tocItem ->
                        // Include content preview if available
                        val contentPreview = tocItem.content?.take(DEFAULT_PREVIEW_CHAR_LENGTH) ?: tocItem.title
                        
                        val segment = TextSegment(
                            text = contentPreview,
                            metadata = mapOf(
                                "type" to "toc",
                                "name" to tocItem.title,
                                "id" to "${file}:${tocItem.title}:${tocItem.level}",
                                "filePath" to file,
                                "level" to tocItem.level
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = tocItem to file
                    }
                }
            }

            is DocQLResult.Chunks -> {
                result.itemsByFile.forEach { (file, chunks) ->
                    chunks.forEach { chunk ->
                        val segment = TextSegment(
                            text = chunk.content,
                            metadata = mapOf(
                                "type" to "chunk",
                                "name" to (chunk.chapterTitle ?: ""),
                                "id" to "${file}:${chunk.content.hashCode()}",
                                "filePath" to file,
                                "startLine" to (chunk.startLine ?: 0),
                                "endLine" to (chunk.endLine ?: 0)
                            )
                        )
                        val item = SearchItem(segment)
                        items.add(item)
                        itemMetadata[item] = chunk to file
                    }
                }
            }

            else -> {} // Ignore other types
        }
    }
    
    /**
     * Get the code content for an entity from source code.
     * 
     * For functions/methods: returns the function body (up to MAX_FUNCTION_PREVIEW_LINES lines)
     * For classes: returns only the class signature and its function/method list (not full content)
     */
    private fun getEntityCodeContent(
        entity: Entity,
        sourceContent: String?,
        docFile: DocumentFile?
    ): String {
        if (sourceContent == null) {
            return getEntitySignature(entity)
        }
        
        val lines = sourceContent.lines()
        val startLine = entity.location.line ?: return getEntitySignature(entity)
        
        return when (entity) {
            is Entity.FunctionEntity, is Entity.ConstructorEntity -> {
                // For functions/methods, extract the function body (up to MAX_FUNCTION_PREVIEW_LINES lines)
                extractFunctionContent(lines, startLine)
            }
            is Entity.ClassEntity -> {
                // For classes, return class signature + list of contained functions
                extractClassSummary(entity, docFile, lines, startLine)
            }
            else -> getEntitySignature(entity)
        }
    }
    
    /**
     * Extract function content from source code lines, limited to MAX_FUNCTION_PREVIEW_LINES.
     */
    private fun extractFunctionContent(lines: List<String>, startLine: Int): String {
        if (startLine <= 0 || startLine > lines.size) {
            return ""
        }
        
        val startIdx = startLine - 1
        var braceCount = 0
        var foundOpenBrace = false
        var endIdx = startIdx
        
        for (i in startIdx until minOf(lines.size, startIdx + MAX_FUNCTION_PREVIEW_LINES * 2)) {
            val line = lines[i]
            
            for (char in line) {
                when (char) {
                    '{' -> {
                        braceCount++
                        foundOpenBrace = true
                    }
                    '}' -> braceCount--
                }
            }
            
            endIdx = i
            
            // If we found the opening brace and now closed all braces, we're done
            if (foundOpenBrace && braceCount == 0) {
                break
            }
        }
        
        // Limit to MAX_FUNCTION_PREVIEW_LINES
        val actualEndIdx = minOf(endIdx, startIdx + MAX_FUNCTION_PREVIEW_LINES - 1)
        val content = lines.subList(startIdx, actualEndIdx + 1).joinToString("\n")
        
        return if (endIdx > actualEndIdx) {
            "$content\n    // ... (${endIdx - actualEndIdx} more lines)"
        } else {
            content
        }
    }
    
    /**
     * Extract class summary: class signature + list of contained functions.
     * This avoids returning the entire class content which can be very large.
     */
    private fun extractClassSummary(
        classEntity: Entity.ClassEntity,
        docFile: DocumentFile?,
        lines: List<String>,
        startLine: Int
    ): String {
        val startIdx = startLine - 1
        if (startIdx < 0 || startIdx >= lines.size) {
            return "class ${classEntity.name}"
        }
        
        // Get class signature (first line or until opening brace)
        val signatureBuilder = StringBuilder()
        var foundOpenBrace = false
        for (i in startIdx until minOf(lines.size, startIdx + 5)) {
            val line = lines[i]
            signatureBuilder.appendLine(line)
            if (line.contains("{")) {
                foundOpenBrace = true
                break
            }
        }
        
        val signature = signatureBuilder.toString().trimEnd()
        
        // Get list of functions in this class from entities
        val classFunctions = docFile?.entities
            ?.filterIsInstance<Entity.FunctionEntity>()
            ?.filter { func ->
                // Simple heuristic: function is in this class if it starts after class start
                // and name pattern suggests it belongs to this class
                val funcLine = func.location.line ?: 0
                funcLine > startLine
            }
            ?.take(20) // Limit to first 20 functions
            ?: emptyList()
        
        return buildString {
            appendLine(signature)
            if (classFunctions.isNotEmpty()) {
                appendLine()
                appendLine("    // Functions:")
                classFunctions.forEach { func ->
                    val sig = func.signature ?: "${func.name}()"
                    val line = func.location.line?.let { ":$it" } ?: ""
                    appendLine("    //   - $sig$line")
                }
            }
            appendLine("}")
        }.trimEnd()
    }
    
    /**
     * Get a readable signature for an entity.
     */
    private fun getEntitySignature(entity: Entity): String {
        return when (entity) {
            is Entity.FunctionEntity -> entity.signature ?: "${entity.name}()"
            is Entity.ConstructorEntity -> entity.signature ?: "${entity.className}()"
            is Entity.ClassEntity -> "class ${entity.name}"
            is Entity.Term -> "${entity.name}: ${entity.definition ?: ""}"
            is Entity.API -> "${entity.name}: ${entity.signature ?: ""}"
        }
    }
}
