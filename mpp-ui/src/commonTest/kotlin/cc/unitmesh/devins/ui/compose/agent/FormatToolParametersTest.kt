package cc.unitmesh.devins.ui.compose.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormatToolParametersTest {

    @Test
    fun `should parse simple JSON query parameter`() {
        val input = """{"query": "authentication"}"""
        val result = formatToolParameters(input)
        assertEquals("query: authentication", result)
    }

    @Test
    fun `should parse JSON with spaces in value`() {
        val input = """{"query": "DevIns Developer Instructions"}"""
        val result = formatToolParameters(input)
        assertEquals("query: DevIns Developer Instructions", result)
    }

    @Test
    fun `should parse JSON with DocQL syntax containing quotes`() {
        val input = """{"query": "$.content.heading(\"Introduction\")"}"""
        val result = formatToolParameters(input)
        assertEquals("""query: $.content.heading("Introduction")""", result)
    }

    @Test
    fun `should parse JSON with multiple parameters`() {
        val input = """{"query": "MCP", "maxResults": 20, "documentPath": "docs/readme.md"}"""
        val result = formatToolParameters(input)
        // Check that all parameters are present
        assertTrue(result.contains("query: MCP"))
        assertTrue(result.contains("maxResults: 20"))
        assertTrue(result.contains("documentPath: docs/readme.md"))
    }

    @Test
    fun `should parse complex DocQL code query`() {
        val input = """{"query": "$.code.class(\"AuthService\")"}"""
        val result = formatToolParameters(input)
        assertEquals("""query: $.code.class("AuthService")""", result)
    }

    @Test
    fun `should parse DocQL with filter expression`() {
        val input = """{"query": "$.code.classes[?(@.name ~= \"Lexer\")]"}"""
        val result = formatToolParameters(input)
        assertEquals("""query: $.code.classes[?(@.name ~= "Lexer")]""", result)
    }

    @Test
    fun `should handle boolean and null values`() {
        val input = """{"enabled": true, "disabled": false, "data": null}"""
        val result = formatToolParameters(input)
        assertTrue(result.contains("enabled: true"))
        assertTrue(result.contains("disabled: false"))
        assertTrue(result.contains("data: null"))
    }

    @Test
    fun `should fallback to original on invalid JSON`() {
        val input = "not json at all"
        val result = formatToolParameters(input)
        assertEquals(input, result)
    }

    @Test
    fun `should parse key=value format`() {
        val input = "path=/some/file.txt mode=read"
        val result = formatToolParameters(input)
        assertTrue(result.contains("path: /some/file.txt"))
        assertTrue(result.contains("mode: read"))
    }
}

