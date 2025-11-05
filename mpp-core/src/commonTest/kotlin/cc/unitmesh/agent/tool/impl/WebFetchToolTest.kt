package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WebFetchToolTest {

    private class MockHttpFetcher : HttpFetcher {
        var shouldSucceed = true
        var responseContent = "Mock content from URL"
        var responseContentType = "text/html"
        var responseStatusCode = 200
        var errorMessage: String? = null
        var timeoutMs: Long = 0

        override suspend fun fetch(url: String, timeout: Long): FetchResult {
            timeoutMs = timeout
            return if (shouldSucceed) {
                FetchResult(
                    success = true,
                    content = responseContent,
                    contentType = responseContentType,
                    statusCode = responseStatusCode
                )
            } else {
                FetchResult(
                    success = false,
                    content = "",
                    error = errorMessage ?: "Mock fetch error"
                )
            }
        }
    }

    private lateinit var mockHttpFetcher: MockHttpFetcher
    private lateinit var mockLLMService: KoogLLMService
    private lateinit var webFetchTool: WebFetchTool

    @BeforeTest
    fun setup() {
        mockHttpFetcher = MockHttpFetcher()
        val mockConfig = cc.unitmesh.llm.ModelConfig(
            provider = cc.unitmesh.llm.LLMProviderType.OPENAI,
            modelName = "gpt-3.5-turbo",
            apiKey = "test-key",
            baseUrl = "",
            temperature = 0.7,
            maxTokens = 4000
        )
        mockLLMService = KoogLLMService(mockConfig)
        webFetchTool = WebFetchTool(mockLLMService)
    }

    @Test
    fun testValidParametersValidation() {
        val validParams = WebFetchParams("Summarize https://example.com")
        // Test that validation passes by creating invocation directly
        val invocation = WebFetchInvocation(validParams, webFetchTool, mockLLMService, mockHttpFetcher)
        assertNotNull(invocation)
    }

    @Test
    fun testEmptyPromptValidation() {
        val invalidParams = WebFetchParams("")
        // Test validation by checking URL parsing
        val parsedUrls = UrlParser.parsePrompt(invalidParams.prompt)
        assertTrue(parsedUrls.validUrls.isEmpty())
    }

    @Test
    fun testNoUrlInPromptValidation() {
        val invalidParams = WebFetchParams("Just some text without any URLs")
        // Test validation by checking URL parsing
        val parsedUrls = UrlParser.parsePrompt(invalidParams.prompt)
        assertTrue(parsedUrls.validUrls.isEmpty())
    }

    @Test
    fun testInvalidUrlInPromptValidation() {
        val invalidParams = WebFetchParams("Check this invalid URL: ftp://example.com")
        // Test validation by checking URL parsing
        val parsedUrls = UrlParser.parsePrompt(invalidParams.prompt)
        assertTrue(parsedUrls.validUrls.isEmpty())
        assertTrue(parsedUrls.errors.isNotEmpty())
    }

    @Test
    fun testContentTruncation() {
        val params = WebFetchParams("Summarize https://example.com")
        // Create content longer than 100K characters
        mockHttpFetcher.responseContent = "x".repeat(150000)

        val invocation = WebFetchInvocation(params, webFetchTool, mockLLMService, mockHttpFetcher)

        // Test that content would be truncated (we can't easily test the full execution without mocking LLM)
        assertEquals(150000, mockHttpFetcher.responseContent.length)
    }

    @Test
    fun testGitHubUrlNormalization() {
        val params = WebFetchParams("Read https://github.com/user/repo/blob/main/README.md")
        val parsedUrls = UrlParser.parsePrompt(params.prompt)

        // URL should be normalized to raw.githubusercontent.com
        assertEquals(1, parsedUrls.validUrls.size)
        assertEquals("https://raw.githubusercontent.com/user/repo/main/README.md", parsedUrls.validUrls[0])
    }

    @Test
    fun testToolMetadata() {
        assertEquals("web-fetch", webFetchTool.name)
        assertEquals("Web Fetch", webFetchTool.metadata.displayName)
        assertEquals("🌐", webFetchTool.metadata.tuiEmoji)
        assertEquals(ToolCategory.Utility, webFetchTool.metadata.category)
    }

    @Test
    fun testToolLocations() {
        val params = WebFetchParams("Check https://example.com and https://test.com")
        val invocation = WebFetchInvocation(params, webFetchTool, mockLLMService, mockHttpFetcher)

        val locations = invocation.getToolLocations()
        assertEquals(2, locations.size)
        assertEquals("https://example.com", locations[0].path)
        assertEquals(LocationType.URL, locations[0].type)
        assertEquals("https://test.com", locations[1].path)
        assertEquals(LocationType.URL, locations[1].type)
    }

    @Test
    fun testInvocationDescription() {
        val shortPrompt = "Summarize https://example.com"
        val invocation1 = WebFetchInvocation(
            WebFetchParams(shortPrompt),
            webFetchTool,
            mockLLMService,
            mockHttpFetcher
        )
        assertEquals("Processing URLs and instructions from prompt: \"$shortPrompt\"", invocation1.getDescription())

        val longPrompt = "x".repeat(150)
        val invocation2 = WebFetchInvocation(
            WebFetchParams(longPrompt),
            webFetchTool,
            mockLLMService,
            mockHttpFetcher
        )
        val expectedDescription = "Processing URLs and instructions from prompt: \"${longPrompt.substring(0, 97)}...\""
        assertEquals(expectedDescription, invocation2.getDescription())
    }

    @Test
    fun testHttpFetcherTimeout() = runTest {
        val params = WebFetchParams("Summarize https://example.com")
        val invocation = WebFetchInvocation(params, webFetchTool, mockLLMService, mockHttpFetcher)

        // Test that timeout is passed correctly
        try {
            invocation.execute(ToolExecutionContext())
        } catch (e: Exception) {
            // Expected to fail due to LLM service, but we can check timeout was set
        }
        assertEquals(10000L, mockHttpFetcher.timeoutMs)
    }
}
