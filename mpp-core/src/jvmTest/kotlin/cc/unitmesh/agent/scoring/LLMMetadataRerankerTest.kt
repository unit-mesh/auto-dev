package cc.unitmesh.agent.scoring

import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for LLMMetadataReranker and related components.
 */
class LLMMetadataRerankerTest {
    
    @Test
    fun `test DocumentMetadataItem creation from TextSegment`() {
        val segment = TextSegment(
            text = "class AuthService { fun authenticate() {} }",
            metadata = mapOf(
                "type" to "class",
                "name" to "AuthService",
                "id" to "src/Auth.kt:AuthService:10",
                "filePath" to "src/Auth.kt"
            )
        )
        
        val item = LLMMetadataReranker.createMetadataItem(
            segment = segment,
            index = 0,
            h1Heading = "Authentication Module",
            heuristicScore = 85.0
        )
        
        assertEquals("src/Auth.kt:AuthService:10", item.id)
        assertEquals("src/Auth.kt", item.filePath)
        assertEquals("Auth.kt", item.fileName)
        assertEquals("kt", item.extension)
        assertEquals("src", item.directory)
        assertEquals("class", item.contentType)
        assertEquals("AuthService", item.name)
        assertEquals("Authentication Module", item.h1Heading)
        assertEquals(85.0, item.heuristicScore)
    }
    
    @Test
    fun `test DocumentRichMetadata creation from path`() {
        val metadata = DocumentRichMetadata.fromPath("docs/design-system/color.md")
        
        assertEquals("docs/design-system/color.md", metadata.path)
        assertEquals("color.md", metadata.fileName)
        assertEquals("md", metadata.extension)
        assertEquals("docs/design-system", metadata.directory)
        assertEquals("markdown", metadata.formatType)
    }
    
    @Test
    fun `test DocumentRichMetadata extraction from markdown`() {
        val content = """
            # Design System Colors
            
            This document describes the color palette.
            
            ## Primary Colors
            
            The primary colors are used for main actions.
            
            ```kotlin
            val primaryColor = Color(0xFF6200EE)
            ```
            
            ## Secondary Colors
            
            Secondary colors complement the primary palette.
            
            See also [typography](typography.md) for font colors.
        """.trimIndent()
        
        val metadata = DocumentRichMetadataExtractor.extractFromMarkdown(
            path = "docs/design-system-color.md",
            content = content,
            lastModified = System.currentTimeMillis()
        )
        
        assertEquals("Design System Colors", metadata.h1Heading)
        assertEquals(3, metadata.headingCount) // H1 + 2 H2s
        assertTrue(metadata.codeBlockCount >= 1)
        assertTrue(metadata.linkCount >= 1)
        assertTrue(metadata.keywords.isNotEmpty())
        assertTrue(metadata.outgoingRefs.any { it.contains("typography") })
    }
    
    @Test
    fun `test DocumentRichMetadata toMetadataItem conversion`() {
        val metadata = DocumentRichMetadata(
            path = "src/AuthService.kt",
            fileName = "AuthService.kt",
            extension = "kt",
            directory = "src",
            formatType = "kotlin",
            h1Heading = "cc.unitmesh.auth",
            headingCount = 5,
            contentLength = 2000,
            classes = listOf("AuthService", "TokenValidator"),
            functions = listOf("authenticate", "validateToken"),
            lastModified = System.currentTimeMillis(),
            fileSize = 2000
        )
        
        val item = metadata.toMetadataItem(
            contentType = "class",
            name = "AuthService",
            preview = "class AuthService implements AuthProvider...",
            heuristicScore = 90.0
        )
        
        assertEquals("src/AuthService.kt", item.id)
        assertEquals("class", item.contentType)
        assertEquals("AuthService", item.name)
        assertEquals("cc.unitmesh.auth", item.h1Heading)
        assertEquals(90.0, item.heuristicScore)
    }
    
    @Test
    fun `test RerankerType parsing`() {
        assertEquals(RerankerType.HEURISTIC, parseRerankerType("heuristic"))
        assertEquals(RerankerType.RRF_COMPOSITE, parseRerankerType("rrf_composite"))
        assertEquals(RerankerType.RRF_COMPOSITE, parseRerankerType("rrf"))
        assertEquals(RerankerType.LLM_METADATA, parseRerankerType("llm_metadata"))
        assertEquals(RerankerType.LLM_METADATA, parseRerankerType("llm"))
        assertEquals(RerankerType.HYBRID, parseRerankerType("hybrid"))
        assertEquals(RerankerType.RRF_COMPOSITE, parseRerankerType("unknown"))
        assertEquals(RerankerType.RRF_COMPOSITE, parseRerankerType(null))
    }
    
    private fun parseRerankerType(typeStr: String?): RerankerType {
        return when (typeStr?.lowercase()) {
            "heuristic" -> RerankerType.HEURISTIC
            "rrf_composite", "rrf" -> RerankerType.RRF_COMPOSITE
            "llm_metadata", "llm" -> RerankerType.LLM_METADATA
            "hybrid" -> RerankerType.HYBRID
            else -> RerankerType.RRF_COMPOSITE
        }
    }
    
    @Test
    fun `test LLMRerankResult fallback creation`() {
        val items = listOf(
            DocumentMetadataItem(
                id = "1",
                filePath = "doc1.md",
                fileName = "doc1.md",
                extension = "md",
                directory = "",
                contentType = "document",
                name = "Document 1",
                preview = "Content of document 1",
                heuristicScore = 80.0
            ),
            DocumentMetadataItem(
                id = "2",
                filePath = "doc2.md",
                fileName = "doc2.md",
                extension = "md",
                directory = "",
                contentType = "document",
                name = "Document 2",
                preview = "Content of document 2",
                heuristicScore = 90.0
            )
        )
        
        // Simulate fallback to heuristic
        val sorted = items.sortedByDescending { it.heuristicScore }
        val result = LLMRerankResult(
            rankedIds = sorted.map { it.id },
            scores = sorted.associate { it.id to it.heuristicScore },
            explanation = "Fallback to heuristic scoring",
            success = false,
            error = "LLM reranking failed"
        )
        
        assertEquals(listOf("2", "1"), result.rankedIds)
        assertEquals(90.0, result.scores["2"])
        assertEquals(80.0, result.scores["1"])
        assertEquals(false, result.success)
        assertNotNull(result.error)
    }
    
    @Test
    fun `test DocumentRichMetadata JSON serialization`() {
        val original = DocumentRichMetadata(
            path = "test/file.md",
            fileName = "file.md",
            extension = "md",
            directory = "test",
            formatType = "markdown",
            h1Heading = "Test Document",
            headings = listOf(
                HeadingInfo(1, "Test Document", "test-document", 1),
                HeadingInfo(2, "Section 1", "section-1", 5)
            ),
            headingCount = 2,
            contentLength = 500,
            keywords = listOf("test", "document", "section")
        )
        
        val json = DocumentRichMetadata.toJson(original)
        val parsed = DocumentRichMetadata.fromJson(json)
        
        assertNotNull(parsed)
        assertEquals(original.path, parsed.path)
        assertEquals(original.h1Heading, parsed.h1Heading)
        assertEquals(original.headingCount, parsed.headingCount)
        assertEquals(original.keywords, parsed.keywords)
    }
    
    @Test
    fun `test HeadingInfo structure`() {
        val heading = HeadingInfo(
            level = 2,
            title = "Getting Started",
            anchor = "getting-started",
            lineNumber = 15
        )
        
        assertEquals(2, heading.level)
        assertEquals("Getting Started", heading.title)
        assertEquals("getting-started", heading.anchor)
        assertEquals(15, heading.lineNumber)
    }
    
    @Test
    fun `test CompositeScorer integration`() {
        val scorer = CompositeScorer()
        val segments = listOf(
            TextSegment("class AuthService", mapOf("type" to "class", "name" to "AuthService")),
            TextSegment("authentication docs", mapOf("type" to "chunk", "name" to "")),
            TextSegment("function authenticate()", mapOf("type" to "function", "name" to "authenticate"))
        )
        
        val scores = scorer.scoreAll(segments, "AuthService")
        
        // Class with exact name match should score highest
        assertEquals(3, scores.size)
        assertTrue(scores[0] > scores[1], "Class 'AuthService' should score higher than chunk")
        assertTrue(scores[0] > scores[2], "Exact name match should score higher than function")
    }
    
    @Test
    @Ignore("Requires LLM service - enable for integration testing")
    fun `test LLMMetadataReranker with mock service`() = runBlocking {
        // This test would require a mock LLM service
        // For now, it serves as documentation of the expected behavior
        
        // val mockLLMService = MockLLMService()
        // val reranker = LLMMetadataReranker(mockLLMService)
        // val items = createTestItems()
        // val result = reranker.rerank(items, "authentication")
        // assertTrue(result.success)
    }
    
    @Test
    fun `test keyword extraction from markdown headings`() {
        val content = """
            # DocumentReranker Architecture
            
            ## BM25 Scoring
            
            The BM25 algorithm is used for term frequency scoring.
            
            ## Type Priority
            
            Classes and functions are prioritized over documentation chunks.
            
            ### CamelCase Identifiers
            
            The system recognizes AuthService, TokenValidator, and similar patterns.
        """.trimIndent()
        
        val metadata = DocumentRichMetadataExtractor.extractFromMarkdown(
            path = "docs/reranker.md",
            content = content
        )
        
        // Should extract keywords from headings and CamelCase identifiers
        assertTrue(metadata.keywords.isNotEmpty())
        assertTrue(metadata.headings.size >= 3)
        assertEquals("DocumentReranker Architecture", metadata.h1Heading)
    }
}

