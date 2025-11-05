package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

/**
 * Parameters for the WebFetch tool
 */
@Serializable
data class WebFetchParams(
    /**
     * A comprehensive prompt that includes the URL(s) to fetch and specific instructions
     * on how to process their content.
     */
    val prompt: String
)

/**
 * Schema for WebFetch tool parameters
 */
object WebFetchSchema : DeclarativeToolSchema(
    description = """
        Processes content from URL(s) embedded in a prompt. Include URLs and instructions 
        (e.g., summarize, extract specific data) directly in the 'prompt' parameter.
        
        The tool will:
        1. Extract URLs from the prompt
        2. Fetch content from those URLs
        3. Use AI to process the content according to the instructions
        4. Return the processed result
        
        Supports local and private network addresses (e.g., localhost).
    """.trimIndent(),
    properties = mapOf(
        "prompt" to string(
            description = """
                A comprehensive prompt that includes the URL(s) to fetch and specific instructions 
                on how to process their content (e.g., "Summarize https://example.com/article and 
                extract key points"). All URLs must be valid and complete, starting with "http://" 
                or "https://", and be fully-formed with a valid hostname (e.g., a domain name like 
                "example.com" or an IP address). For example, "https://example.com" is valid, 
                but "example.com" is not.
            """.trimIndent(),
            required = true
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """
            /$toolName
            ```json
            {"prompt": "Summarize the content from https://example.com and highlight the main topics"}
            ```
        """.trimIndent()
    }
}

/**
 * Result of URL parsing from a prompt
 */
data class ParsedUrls(
    val validUrls: List<String>,
    val errors: List<String>
)

/**
 * Utility object for URL parsing and validation
 */
object UrlParser {
    /**
     * Parses a prompt to extract valid URLs and identify malformed ones.
     */
    fun parsePrompt(text: String): ParsedUrls {
        val validUrls = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Use regex to find URLs in the text, even if they're embedded in other text
        val urlPattern = Regex("""https?://[^\s\u4e00-\u9fff]+""")
        val matches = urlPattern.findAll(text)

        for (match in matches) {
            val potentialUrl = match.value
            try {
                // Clean up the URL (remove trailing punctuation that might not be part of URL)
                val cleanUrl = potentialUrl.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':')
                val url = normalizeUrl(cleanUrl)

                // Check protocol (should already be http/https from regex)
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    validUrls.add(url)
                } else {
                    errors.add("Unsupported protocol in URL: \"$cleanUrl\". Only http and https are supported.")
                }
            } catch (e: Exception) {
                errors.add("Malformed URL detected: \"$potentialUrl\".")
            }
        }

        // Fallback: if no URLs found with regex, try the old token-based approach
        if (validUrls.isEmpty() && errors.isEmpty()) {
            val tokens = text.split(Regex("\\s+"))
            for (token in tokens) {
                if (token.isBlank()) continue

                // Heuristic to check if the token appears to contain URL-like chars
                if (token.contains("://")) {
                    try {
                        // Basic URL validation
                        val url = normalizeUrl(token)

                        // Check protocol
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            validUrls.add(url)
                        } else {
                            errors.add("Unsupported protocol in URL: \"$token\". Only http and https are supported.")
                        }
                    } catch (e: Exception) {
                        errors.add("Malformed URL detected: \"$token\".")
                    }
                }
            }
        }

        return ParsedUrls(validUrls, errors)
    }

    /**
     * Normalize URL (basic validation and cleanup)
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()

        // Convert GitHub blob URLs to raw URLs
        if (normalized.contains("github.com") && normalized.contains("/blob/")) {
            normalized = normalized
                .replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
        }

        return normalized
    }
}

/**
 * Tool invocation for WebFetch
 */
class WebFetchInvocation(
    params: WebFetchParams,
    tool: WebFetchTool,
    private val llmService: KoogLLMService,
    private val httpFetcher: HttpFetcher
) : BaseToolInvocation<WebFetchParams, ToolResult>(params, tool) {

    override fun getDescription(): String {
        val displayPrompt = if (params.prompt.length > 100) {
            params.prompt.substring(0, 97) + "..."
        } else {
            params.prompt
        }
        return "Processing URLs and instructions from prompt: \"$displayPrompt\""
    }

    override fun getToolLocations(): List<ToolLocation> {
        val parsedUrls = UrlParser.parsePrompt(params.prompt)
        return parsedUrls.validUrls.map { ToolLocation(it, LocationType.URL) }
    }

    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        return ToolErrorUtils.safeExecute(ToolErrorType.WEB_FETCH_PROCESSING_ERROR) {
            val parsedUrls = UrlParser.parsePrompt(params.prompt)

            if (parsedUrls.validUrls.isEmpty()) {
                throw ToolException(
                    "No valid URLs found in prompt. URLs must start with http:// or https://",
                    ToolErrorType.INVALID_PARAMETERS
                )
            }

            // Use fallback method: fetch content directly and use AI to process
            executeFallback(parsedUrls.validUrls.first())
        }
    }

    /**
     * Fallback execution: fetch content directly and use AI to process it
     */
    private suspend fun executeFallback(url: String): ToolResult {
        try {
            // Fetch content from URL
            val fetchResult = httpFetcher.fetch(url, timeout = 10000)

            if (!fetchResult.success) {
                throw ToolException(
                    "Failed to fetch URL: ${fetchResult.error}",
                    ToolErrorType.WEB_FETCH_FAILED
                )
            }

            // Truncate content if too long (max 100K chars)
            val content = fetchResult.content.take(100000)

            // Use AI to process the content according to the original prompt
            val fallbackPrompt = """
                The user requested the following: "${params.prompt}".
                
                I have fetched the content of the page. Please use the following content to answer the request.
                Do not attempt to access the URL again.
                
                ---
                $content
                ---
            """.trimIndent()

            // Use LLM to process the content
            val result = llmService.streamPrompt(
                userPrompt = fallbackPrompt,
                compileDevIns = false
            ).first()

            return ToolResult.Success(
                content = result,
                metadata = mapOf(
                    "url" to url,
                    "contentLength" to content.length.toString(),
                    "method" to "fallback_fetch"
                )
            )
        } catch (e: ToolException) {
            throw e
        } catch (e: Exception) {
            throw ToolException(
                "Error during web fetch for $url: ${e.message}",
                ToolErrorType.WEB_FETCH_FAILED
            )
        }
    }
}

/**
 * HTTP Fetcher interface for fetching web content
 * This allows platform-specific implementations (JVM, JS, Native)
 */
interface HttpFetcher {
    /**
     * Fetch content from a URL
     *
     * @param url The URL to fetch
     * @param timeout Timeout in milliseconds
     * @return FetchResult containing the content or error
     */
    suspend fun fetch(url: String, timeout: Long = 10000): FetchResult
}

/**
 * Result of an HTTP fetch operation
 */
data class FetchResult(
    val success: Boolean,
    val content: String,
    val contentType: String = "",
    val statusCode: Int = 0,
    val error: String? = null
)

/**
 * WebFetch tool for fetching and processing web content using AI
 *
 * This tool combines web fetching with AI processing to:
 * 1. Fetch content from URLs
 * 2. Process the content according to user instructions
 * 3. Return AI-generated summaries or extractions
 *
 * The tool automatically creates its own HttpFetcher using platform-specific engines.
 */
class WebFetchTool(
    private val llmService: KoogLLMService
) : BaseExecutableTool<WebFetchParams, ToolResult>() {

    // Create platform-specific HTTP fetcher internally
    private val httpFetcher: HttpFetcher by lazy {
        KtorHttpFetcher.create()
    }

    override val name: String = "web-fetch"
    override val description: String =
        "Processes content from URL(s), including local and private network addresses (e.g., localhost), embedded in a prompt. Include up to 20 URLs and instructions (e.g., summarize, extract specific data) directly in the 'prompt' parameter.".trimIndent()

    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Web Fetch",
        tuiEmoji = "🌐",
        composeIcon = "language",
        category = ToolCategory.Utility,
        schema = WebFetchSchema
    )

    override fun getParameterClass(): String = WebFetchParams::class.simpleName ?: "WebFetchParams"

    override fun createToolInvocation(params: WebFetchParams): ToolInvocation<WebFetchParams, ToolResult> {
        validateParameters(params)
        return WebFetchInvocation(params, this, llmService, httpFetcher)
    }

    private fun validateParameters(params: WebFetchParams) {
        if (params.prompt.isBlank()) {
            throw ToolException(
                "The 'prompt' parameter cannot be empty and must contain URL(s) and instructions.",
                ToolErrorType.MISSING_REQUIRED_PARAMETER
            )
        }

        val parsedUrls = UrlParser.parsePrompt(params.prompt)

        if (parsedUrls.errors.isNotEmpty()) {
            throw ToolException(
                "Error(s) in prompt URLs:\n- ${parsedUrls.errors.joinToString("\n- ")}",
                ToolErrorType.INVALID_PARAMETERS
            )
        }

        if (parsedUrls.validUrls.isEmpty()) {
            throw ToolException(
                "The 'prompt' must contain at least one valid URL (starting with http:// or https://).",
                ToolErrorType.INVALID_PARAMETERS
            )
        }
    }
}

