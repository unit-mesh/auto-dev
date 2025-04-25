package cc.unitmesh.devti.agent.custom

import kotlin.test.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

class CustomAgentExecutorTest {

    @Test
    fun `test replacePlaceholders with simple content placeholder`() {
        val input = """{"messages": [{"role": "user", "content":"${'$'}content"}]}"""
        val expected = """{"messages": [{"role": "user", "content": "Hello world"}]}"""
        
        val result = CustomAgentExecutor.replacePlaceholders(input, "Hello world")
        
        assertEquals(expected, result)
    }

    @Test
    fun `test replacePlaceholders with JSON value placeholder`() {
        val input = """{"query": "${'$'}content", "options": {"key": "value"}}"""
        val expected = """{"query": "Hello world", "options": {"key": "value"}}"""
        
        val result = CustomAgentExecutor.replacePlaceholders(input, "Hello world")
        
        assertEquals(expected, result)
    }

    @Test
    fun `test replacePlaceholders with no placeholder`() {
        val input = """{"messages": [{"role": "user", "content": "Static content"}]}"""
        val expected = input // Should remain unchanged
        
        val result = CustomAgentExecutor.replacePlaceholders(input, "Hello world")
        
        assertEquals(expected, result)
    }

    @Test
    fun `test replacePlaceholders with multiple placeholders`() {
        val input = """{"query": "${'$'}content""""
        val expected = """{"query": "Hello world""""
        
        val result = CustomAgentExecutor.replacePlaceholders(input, "Hello world")
        
        assertEquals(expected, result)
    }

    @Test
    fun `test replacePlaceholders with special characters in promptText`() {
        val input = """{"messages": [{"role": "user", "content":"${'$'}content"}]}"""
        val promptText = "Hello \"world\" with quotes and \\ backslashes"
        
        val result = CustomAgentExecutor.replacePlaceholders(input, promptText)
        
        assertTrue(result.contains(promptText))
    }
}
