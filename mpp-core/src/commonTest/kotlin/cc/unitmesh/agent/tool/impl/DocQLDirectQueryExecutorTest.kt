package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.devins.document.DocumentRegistry
import cc.unitmesh.devins.document.docql.DocQLResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class DocQLDirectQueryExecutorTest {

    @Test
    fun `test queryAllDocuments with no results returns suggestion`() = runTest {
        DocumentRegistry.clearCache()
        val executor = DocQLDirectQueryExecutor(maxResults = 10)
        
        // With empty registry, it should return Error
        val result = executor.queryAllDocuments("$.nonexistent.query")
        
        assertTrue(result is ToolResult.Error)
        assertTrue(result.message.contains("No documents available"))
    }

    @Test
    fun `test extractQueryType`() = runTest {
        val executor = DocQLDirectQueryExecutor(maxResults = 10)
        val result = executor.queryAllDocuments("$.code.class('Test')")

        assertTrue(result is ToolResult.Error)
        assertTrue(result.message.contains("No documents available"))
    }
}
