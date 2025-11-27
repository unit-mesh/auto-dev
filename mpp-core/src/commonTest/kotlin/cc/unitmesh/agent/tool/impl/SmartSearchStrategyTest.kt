package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.scoring.DocumentReranker
import cc.unitmesh.agent.scoring.DocumentRerankerConfig
import cc.unitmesh.agent.scoring.ExpandedKeywords
import cc.unitmesh.agent.scoring.KeywordExpander
import cc.unitmesh.agent.scoring.KeywordExpanderConfig
import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.impl.docql.ExpandStrategy
import cc.unitmesh.agent.tool.impl.docql.FilterStrategy
import cc.unitmesh.agent.tool.impl.docql.KeepStrategy
import cc.unitmesh.agent.tool.impl.docql.SearchItem
import cc.unitmesh.agent.tool.impl.docql.SearchLevelResult
import cc.unitmesh.agent.tool.impl.docql.SmartSearchContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmartSearchStrategyTest {

    private val config = DocumentRerankerConfig()
    private val reranker = DocumentReranker(config)
    private val expanderConfig = KeywordExpanderConfig(minResultsThreshold = 2)
    private val keywordExpander = KeywordExpander(expanderConfig)

    private fun createSearchItem(text: String, name: String = "test"): SearchItem {
        return SearchItem(
            segment = TextSegment(
                text = text,
                metadata = mapOf("name" to name, "type" to "test")
            )
        )
    }

    private fun createLevelResult(items: List<SearchItem>): SearchLevelResult {
        return SearchLevelResult(
            items = items,
            totalCount = items.size,
            truncated = false,
            metadata = mutableMapOf(),
            docsSearched = 1,
            activeChannels = listOf("test")
        )
    }

    @Test
    fun testKeepStrategy() = runTest {
        val strategy = KeepStrategy()
        val items = listOf(createSearchItem("item1"))
        val level1Results = createLevelResult(items)

        var resultBuilderCalled = false
        val context = SmartSearchContext(
            keyword = "test",
            secondaryKeyword = null,
            documentPath = null,
            maxResults = 10,
            reranker = reranker,
            rerankerConfig = config,
            keywordExpander = keywordExpander,
            expandedKeywords = ExpandedKeywords("test", listOf("test"), emptyList(), emptyList()),
            level1Results = level1Results,
            searchExecutor = { _, _ -> createLevelResult(emptyList()) },
            resultBuilder = { res, _, stats ->
                resultBuilderCalled = true
                assertEquals(1, res.totalCount)
                assertEquals("KEEP", stats.strategyUsed)
                assertEquals(1, stats.levelUsed)
                ToolResult.Success("Success")
            },
            fallbackExecutor = { _, _, _, _, _ -> ToolResult.Error("Fallback") }
        )

        strategy.execute(context)
        assertTrue(resultBuilderCalled)
    }

    @Test
    fun testFilterStrategy_Success() = runTest {
        val strategy = FilterStrategy()
        val items = listOf(
            createSearchItem("apple pie"),
            createSearchItem("blueberry pie"),
            createSearchItem("banana split"),
            createSearchItem("apple tart")
        )
        val level1Results = createLevelResult(items)

        var resultBuilderCalled = false
        val context = SmartSearchContext(
            keyword = "apple",
            secondaryKeyword = "pie",
            documentPath = null,
            maxResults = 10,
            reranker = reranker,
            rerankerConfig = config,
            keywordExpander = keywordExpander,
            expandedKeywords = ExpandedKeywords("apple", listOf("apple"), listOf("pie"), emptyList()),
            level1Results = level1Results,
            searchExecutor = { _, _ -> createLevelResult(emptyList()) },
            resultBuilder = { res, _, stats ->
                resultBuilderCalled = true
                assertEquals(2, res.totalCount) // "apple pie" and "blueberry pie" should match
                assertEquals("apple pie", res.items.first().segment.text)
                assertEquals("FILTER", stats.strategyUsed)
                assertEquals(2, stats.levelUsed)
                ToolResult.Success("Success")
            },
            fallbackExecutor = { _, _, _, _, _ -> ToolResult.Error("Fallback") }
        )

        strategy.execute(context)
        assertTrue(resultBuilderCalled)
    }

    @Test
    fun testFilterStrategy_FallbackToTruncated() = runTest {
        val strategy = FilterStrategy()
        val items = listOf(
            createSearchItem("apple"),
            createSearchItem("banana")
        )
        val level1Results = createLevelResult(items)

        // Filter keyword "orange" won't match anything
        var resultBuilderCalled = false
        val context = SmartSearchContext(
            keyword = "apple",
            secondaryKeyword = "orange",
            documentPath = null,
            maxResults = 10,
            reranker = reranker,
            rerankerConfig = config,
            keywordExpander = keywordExpander,
            expandedKeywords = ExpandedKeywords("apple", listOf("apple"), listOf("orange"), emptyList()),
            level1Results = level1Results,
            searchExecutor = { _, _ -> createLevelResult(emptyList()) },
            resultBuilder = { res, _, stats ->
                resultBuilderCalled = true
                // Should fall back to original results (truncated logic simulation)
                assertEquals(2, res.totalCount)
                assertEquals("KEEP_TRUNCATED", stats.strategyUsed)
                assertEquals(1, stats.levelUsed)
                ToolResult.Success("Success")
            },
            fallbackExecutor = { _, _, _, _, _ -> ToolResult.Error("Fallback") }
        )

        strategy.execute(context)
        assertTrue(resultBuilderCalled)
    }

    @Test
    fun testExpandStrategy_Level2() = runTest {
        val strategy = ExpandStrategy()
        val level1Results = createLevelResult(emptyList()) // Empty level 1

        var resultBuilderCalled = false
        val context = SmartSearchContext(
            keyword = "test",
            secondaryKeyword = null,
            documentPath = null,
            maxResults = 10,
            reranker = reranker,
            rerankerConfig = config,
            keywordExpander = keywordExpander,
            expandedKeywords = ExpandedKeywords("test", listOf("test"), listOf("expanded"), emptyList()),
            level1Results = level1Results,
            searchExecutor = { keywords, _ ->
                if (keywords.contains("expanded")) {
                    createLevelResult(listOf(createSearchItem("expanded result"), createSearchItem("another result")))
                } else {
                    createLevelResult(emptyList())
                }
            },
            resultBuilder = { res, _, stats ->
                resultBuilderCalled = true
                assertEquals(2, res.totalCount)
                assertEquals("EXPAND", stats.strategyUsed)
                assertEquals(2, stats.levelUsed)
                ToolResult.Success("Success")
            },
            fallbackExecutor = { _, _, _, _, _ -> ToolResult.Error("Fallback") }
        )

        strategy.execute(context)
        assertTrue(resultBuilderCalled)
    }

    @Test
    fun testExpandStrategy_Fallback() = runTest {
        val strategy = ExpandStrategy()
        val level1Results = createLevelResult(emptyList())

        var fallbackCalled = false
        val context = SmartSearchContext(
            keyword = "test",
            secondaryKeyword = null,
            documentPath = null,
            maxResults = 10,
            reranker = reranker,
            rerankerConfig = config,
            keywordExpander = keywordExpander,
            expandedKeywords = ExpandedKeywords("test", listOf("test"), listOf("expanded"), listOf("more")),
            level1Results = level1Results,
            searchExecutor = { _, _ -> createLevelResult(emptyList()) }, // Always return empty
            resultBuilder = { _, _, _ -> ToolResult.Success("Should not be called") },
            fallbackExecutor = { _, _, _, _, _ ->
                fallbackCalled = true
                ToolResult.Success("Fallback")
            }
        )

        strategy.execute(context)
        assertTrue(fallbackCalled)
    }
}
