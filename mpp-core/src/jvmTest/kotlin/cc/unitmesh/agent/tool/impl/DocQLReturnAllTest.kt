package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.impl.docql.DocQLDirectQueryExecutor
import cc.unitmesh.devins.document.*
import cc.unitmesh.devins.document.docql.DocQLResult
import cc.unitmesh.devins.document.docql.parseDocQL
import cc.unitmesh.devins.document.docql.DocQLExecutor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Test suite for DocQL returnAll parameter functionality.
 * 
 * Verifies that:
 * 1. returnAll=true returns all results without truncation
 * 2. returnAll=false (default) truncates results to maxResults
 */
class DocQLReturnAllTest {
    
    private val sampleKotlinCode = """
        package cc.unitmesh.sample
        
        class ClassA { }
        class ClassB { }
        class ClassC { }
        class ClassD { }
        class ClassE { }
        class ClassF { }
        class ClassG { }
        class ClassH { }
        class ClassI { }
        class ClassJ { }
        class ClassK { }
        class ClassL { }
        class ClassM { }
        class ClassN { }
        class ClassO { }
        class ClassP { }
        class ClassQ { }
        class ClassR { }
        class ClassS { }
        class ClassT { }
        class ClassU { }
        class ClassV { }
        class ClassW { }
        class ClassX { }
        class ClassY { }
        class ClassZ { }
        
        fun functionA() { }
        fun functionB() { }
        fun functionC() { }
        fun functionD() { }
        fun functionE() { }
    """.trimIndent()
    
    @Before
    fun setUp() {
        // Clear cache before each test
        DocumentRegistry.clearCache()
    }
    
    @After
    fun tearDown() {
        // Clear cache after each test
        DocumentRegistry.clearCache()
    }
    
    @Test
    fun `returnAll should return all classes without truncation`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Sample.kt",
            path = "test/Sample.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.class("*") which should return all classes
        val query = parseDocQL("$.code.class(\"*\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Entities, "Expected Entities result but got: $result")
        val entities = result as DocQLResult.Entities
        
        // Should have 26 classes (ClassA through ClassZ)
        println("✅ $.code.class(\"*\") found ${entities.totalCount} classes")
        assertTrue(entities.totalCount >= 26, "Expected at least 26 classes, got ${entities.totalCount}")
    }
    
    @Test
    fun `returnAll executor should not truncate results`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Sample.kt",
            path = "test/Sample.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Register document
        DocumentRegistry.registerDocument("test/Sample.kt", parsedFile, parser)
        
        // Test with returnAll=true
        val executorReturnAll = DocQLDirectQueryExecutor(maxResults = 5, returnAll = true)
        val resultAll = executorReturnAll.queryAllDocuments("$.code.classes[*]")
        
        // Extract content from ToolResult
        val content = when (resultAll) {
            is ToolResult.Success -> resultAll.content
            is ToolResult.Error -> resultAll.message
            is ToolResult.AgentResult -> resultAll.content
        }
        
        println("returnAll=true result: ${content.take(500)}")
        
        // Should NOT be truncated
        assertFalse(
            content.contains("more available"),
            "returnAll=true should not show 'more available' truncation message"
        )
        
        // Verify it mentions all classes, not just maxResults
        val classCountMatch = Regex("Found (\\d+) entities").find(content)
        val classCount = classCountMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        assertTrue(classCount >= 26, "returnAll=true should report all 26+ classes, got $classCount")
        
        println("✅ returnAll=true correctly returned all $classCount classes without truncation")
    }
    
    @Test
    fun `default executor should truncate results`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Sample.kt",
            path = "test/Sample.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Register document
        DocumentRegistry.registerDocument("test/Sample.kt", parsedFile, parser)
        
        // Test with returnAll=false (default behavior)
        val executorDefault = DocQLDirectQueryExecutor(maxResults = 5, returnAll = false)
        val resultDefault = executorDefault.queryAllDocuments("$.code.classes[*]")
        
        // Extract content from ToolResult
        val content = when (resultDefault) {
            is ToolResult.Success -> resultDefault.content
            is ToolResult.Error -> resultDefault.message
            is ToolResult.AgentResult -> resultDefault.content
        }
        
        println("returnAll=false result: ${content.take(500)}")
        
        // Should be truncated since we have 26 classes but maxResults=5
        assertTrue(
            content.contains("more available"),
            "returnAll=false should show 'more available' truncation message"
        )
        
        println("✅ returnAll=false correctly truncated results")
    }
    
    @Test
    fun `DocQLParams returnAll parameter works correctly`() {
        // Test that DocQLParams correctly parses returnAll
        val paramsWithReturnAll = DocQLParams(
            query = "$.code.classes[*]",
            returnAll = true
        )
        assertTrue(paramsWithReturnAll.returnAll == true)
        
        val paramsWithoutReturnAll = DocQLParams(
            query = "$.code.classes[*]"
        )
        assertTrue(paramsWithoutReturnAll.returnAll == false || paramsWithoutReturnAll.returnAll == null)
        
        println("✅ DocQLParams returnAll parameter parsed correctly")
    }
}

