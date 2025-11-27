package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.devins.document.DocumentChunk
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.Entity
import cc.unitmesh.devins.document.docql.DocQLResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

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
     */
    fun collectSearchItems(
        result: DocQLResult,
        items: MutableList<SearchItem>,
        itemMetadata: MutableMap<SearchItem, Pair<Any, String?>>
    ) {
        when (result) {
            is DocQLResult.Entities -> {
                result.itemsByFile.forEach { (file, entities) ->
                    entities.forEach { entity ->
                        val type = when (entity) {
                            is Entity.ClassEntity -> "class"
                            is Entity.FunctionEntity -> "function"
                            else -> "entity"
                        }
                        val segment = TextSegment(
                            text = entity.name,
                            metadata = mapOf(
                                "type" to type,
                                "name" to entity.name,
                                "id" to "${file}:${entity.name}:${entity.location.line}",
                                "filePath" to file
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
                        val segment = TextSegment(
                            text = tocItem.title,
                            metadata = mapOf(
                                "type" to "toc",
                                "name" to tocItem.title,
                                "id" to "${file}:${tocItem.title}:${tocItem.level}",
                                "filePath" to file
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
                                "id" to "${file}:${chunk.content.hashCode()}",
                                "filePath" to file
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
}
