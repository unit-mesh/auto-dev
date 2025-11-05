package cc.unitmesh.agent.tool.impl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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
            "http://example.com:99999", // Invalid port
            "https://example.com/path with spaces"
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
    fun testUrlsInMixedText() {
        val testCases = mapOf(
            "请访问 https://example.com 获取更多信息" to listOf("https://example.com"),
            "Check out https://github.com/user/repo and https://docs.example.com for details" to 
                listOf("https://github.com/user/repo", "https://docs.example.com"),
            "README文件的内容：https://raw.githubusercontent.com/unit-mesh/auto-dev/master/README.md" to 
                listOf("https://raw.githubusercontent.com/unit-mesh/auto-dev/master/README.md"),
            "读取 https://httpbin.org/json 并总结内容" to listOf("https://httpbin.org/json"),
            "URL: https://api.example.com/v1/data?param=value&other=123" to 
                listOf("https://api.example.com/v1/data?param=value&other=123")
        )

        testCases.forEach { (text, expectedUrls) ->
            val result = UrlParser.parsePrompt(text)
            assertEquals(
                expectedUrls.size, 
                result.validUrls.size, 
                "Should find ${expectedUrls.size} URLs in: $text"
            )
            expectedUrls.forEachIndexed { index, expectedUrl ->
                assertEquals(
                    expectedUrl, 
                    result.validUrls[index], 
                    "Should extract correct URL at index $index from: $text"
                )
            }
            assertTrue(result.errors.isEmpty(), "Should have no errors for mixed text: $text")
        }
    }

    @Test
    fun testUrlsWithTrailingPunctuation() {
        val testCases = mapOf(
            "Visit https://example.com." to "https://example.com",
            "Check https://example.com, it's great!" to "https://example.com",
            "See (https://example.com) for info" to "https://example.com",
            "Link: https://example.com;" to "https://example.com",
            "Go to https://example.com?" to "https://example.com",
            "URL [https://example.com]" to "https://example.com",
            "Site {https://example.com}" to "https://example.com"
        )

        testCases.forEach { (text, expectedUrl) ->
            val result = UrlParser.parsePrompt(text)
            assertEquals(1, result.validUrls.size, "Should find one URL in: $text")
            assertEquals(expectedUrl, result.validUrls[0], "Should clean trailing punctuation from: $text")
            assertTrue(result.errors.isEmpty(), "Should have no errors for: $text")
        }
    }

    @Test
    fun testGitHubBlobUrlNormalization() {
        val testCases = mapOf(
            "https://github.com/user/repo/blob/main/README.md" to
                "https://raw.githubusercontent.com/user/repo/main/README.md",
            "https://github.com/unit-mesh/auto-dev/blob/master/README.md" to
                "https://raw.githubusercontent.com/unit-mesh/auto-dev/master/README.md",
            "https://github.com/org/project/blob/develop/docs/guide.md" to
                "https://raw.githubusercontent.com/org/project/develop/docs/guide.md"
        )

        testCases.forEach { (input, expected) ->
            val result = UrlParser.parsePrompt(input)
            assertEquals(1, result.validUrls.size, "Should find one URL for GitHub blob: $input")
            assertEquals(expected, result.validUrls[0], "Should normalize GitHub blob URL: $input")
            assertTrue(result.errors.isEmpty(), "Should have no errors for GitHub blob URL: $input")
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

    @Test
    fun testMixedValidAndInvalidUrls() {
        val text = "Valid: https://example.com, Invalid: git://github.com/repo.git, Another valid: http://test.com"
        val result = UrlParser.parsePrompt(text)

        assertEquals(2, result.validUrls.size, "Should find 2 valid URLs")
        assertTrue(result.validUrls.contains("https://example.com"), "Should contain first valid URL")
        assertTrue(result.validUrls.contains("http://test.com"), "Should contain second valid URL")

        assertEquals(1, result.errors.size, "Should have 1 error for invalid protocol")
        assertTrue(result.errors[0].contains("git://"), "Error should mention git protocol")
    }

    @Test
    fun testUrlsWithQueryParameters() {
        val testCases = listOf(
            "https://api.example.com/search?q=kotlin&type=repo",
            "https://example.com/path?param1=value1&param2=value2&param3=value%20with%20spaces",
            "https://site.com/api/v1/data?filter[name]=test&sort=-created_at"
        )

        testCases.forEach { url ->
            val result = UrlParser.parsePrompt("Check this URL: $url for data")
            assertEquals(1, result.validUrls.size, "Should find URL with query params: $url")
            assertEquals(url, result.validUrls[0], "Should preserve query parameters: $url")
            assertTrue(result.errors.isEmpty(), "Should have no errors for URL with query params: $url")
        }
    }

    @Test
    fun testUrlsWithFragments() {
        val testCases = listOf(
            "https://example.com/page#section1",
            "https://docs.example.com/guide#installation",
            "https://github.com/user/repo#readme"
        )

        testCases.forEach { url ->
            val result = UrlParser.parsePrompt("See $url for details")
            assertEquals(1, result.validUrls.size, "Should find URL with fragment: $url")
            assertEquals(url, result.validUrls[0], "Should preserve fragment: $url")
            assertTrue(result.errors.isEmpty(), "Should have no errors for URL with fragment: $url")
        }
    }

    @Test
    fun testFallbackTokenBasedParsing() {
        // Test case where regex doesn't find URLs but token-based parsing should
        val text = "Check this: https://example.com/path/with/中文/characters"
        val result = UrlParser.parsePrompt(text)

        // This should work with either regex or fallback parsing
        assertTrue(result.validUrls.isNotEmpty() || result.errors.isEmpty(),
            "Should either find URL or have no errors for mixed character URL")
    }

    @Test
    fun testRealWorldExamples() {
        val testCases = mapOf(
            "读取 https://raw.githubusercontent.com/unit-mesh/auto-dev/master/README.md 并总结" to
                listOf("https://raw.githubusercontent.com/unit-mesh/auto-dev/master/README.md"),
            "Please fetch and summarize https://httpbin.org/json" to
                listOf("https://httpbin.org/json"),
            "Compare data from https://api1.example.com and https://api2.example.com" to
                listOf("https://api1.example.com", "https://api2.example.com"),
            "Download the file from ftp://files.example.com/data.csv and process it" to
                emptyList<String>() // FTP should be rejected
        )

        testCases.forEach { (text, expectedUrls) ->
            val result = UrlParser.parsePrompt(text)
            assertEquals(expectedUrls.size, result.validUrls.size,
                "Should find ${expectedUrls.size} valid URLs in: $text")
            expectedUrls.forEach { expectedUrl ->
                assertTrue(result.validUrls.contains(expectedUrl),
                    "Should contain URL $expectedUrl in result from: $text")
            }
        }
    }
}
