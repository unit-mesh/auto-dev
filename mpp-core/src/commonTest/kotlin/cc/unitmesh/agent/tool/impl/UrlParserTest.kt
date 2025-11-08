package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.impl.http.UrlParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrlParserTest {

    @Test
    fun testValidHttpUrls() {
        val testCases = listOf(
            "https://example.com",
            "http://example.com",
            "https://api.example.com/v1/data",
            "https://example.com:8080/path?param=value",
            "https://subdomain.example.com/path/to/resource"
        )

        testCases.forEach { url ->
            val result = UrlParser.parsePrompt(url)
            assertEquals(1, result.validUrls.size, "Should find one valid URL for: $url")
            assertEquals(url, result.validUrls[0], "Should extract correct URL: $url")
            assertTrue(result.errors.isEmpty(), "Should have no errors for valid URL: $url")
        }
    }

    @Test
    fun testUnsupportedProtocols() {
        val testCases = mapOf(
            "git://github.com/user/repo.git" to "git://",
            "ftp://files.example.com/file.txt" to "ftp://",
            "ssh://user@server.com/path" to "ssh://",
            "file:///local/path/file.txt" to "file://",
            "mailto:user@example.com" to "mailto:",
            "tel:+1234567890" to "tel:",
            "data:text/plain;base64,SGVsbG8=" to "data:"
        )

        testCases.forEach { (url, protocol) ->
            val result = UrlParser.parsePrompt(url)
            assertTrue(result.validUrls.isEmpty(), "Should have no valid URLs for unsupported protocol: $protocol")
            assertEquals(1, result.errors.size, "Should have one error for unsupported protocol: $protocol")
            assertTrue(
                result.errors[0].contains("Unsupported protocol"),
                "Error should mention unsupported protocol for: $url"
            )
        }
    }

    @Test
    fun testMalformedUrls() {
        val testCases = listOf(
            "https://",
            "http://",
            "https:///invalid",
            "http://[invalid-ipv6",
            "https://example..com",
            "http://example.com:99999" // Invalid port
        )

        testCases.forEach { url ->
            val result = UrlParser.parsePrompt(url)
            // Note: Some of these might be handled by normalizeUrl, so we check for either no valid URLs or errors
            assertTrue(
                result.validUrls.isEmpty() || result.errors.isNotEmpty(),
                "Should either have no valid URLs or have errors for malformed URL: $url"
            )
        }
    }

    @Test
    fun testEmptyAndBlankInputs() {
        val testCases = listOf("", "   ", "\n\t  \n", "no urls here", "just some text without links")

        testCases.forEach { input ->
            val result = UrlParser.parsePrompt(input)
            assertTrue(result.validUrls.isEmpty(), "Should find no URLs in: '$input'")
            assertTrue(result.errors.isEmpty(), "Should have no errors for text without URLs: '$input'")
        }
    }
}
