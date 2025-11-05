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

        // Use broader regex to find all potential URLs (any protocol)
        // This matches both "protocol://" and "protocol:" patterns
        val urlPattern = Regex("""[a-zA-Z][a-zA-Z0-9+.-]*:(?://)?[^\s]+""")
        val matches = urlPattern.findAll(text)

        for (match in matches) {
            val potentialUrl = match.value
            try {
                // Clean up the URL (remove trailing punctuation that might not be part of URL)
                val cleanUrl = potentialUrl.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':')

                // Check for URLs with spaces by looking at the context around the match
                val matchStart = match.range.first
                val matchEnd = match.range.last + 1

                // Look for a pattern like "protocol://domain/path with spaces" where our regex only matched up to the space
                // We do this by checking if there are non-whitespace characters immediately after our match that could be part of a URL
                var extendedUrl = cleanUrl
                if (matchEnd < text.length) {
                    val remainingText = text.substring(matchEnd)
                    // Check if the remaining text starts with a space followed by URL-like characters (not just any text)
                    // We want to catch cases like "https://example.com/path with spaces" but not "https://example.com 获取更多信息"
                    val spacePattern = Regex("""^(\s+[a-zA-Z0-9._~:/?#[\]@!$&'()*+,;=-]+)+""")
                    val spaceMatch = spacePattern.find(remainingText)
                    if (spaceMatch != null && spaceMatch.value.contains(" ")) {
                        // This looks like a URL with spaces in it (not just followed by other text)
                        extendedUrl = cleanUrl + spaceMatch.value
                        errors.add("Malformed URL detected: \"$extendedUrl\".")
                        continue
                    }
                }

                // Check protocol first
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    // Validate the URL structure
                    if (isValidHttpUrl(cleanUrl)) {
                        val url = normalizeUrl(cleanUrl)
                        validUrls.add(url)
                    } else {
                        errors.add("Malformed URL detected: \"$cleanUrl\".")
                    }
                } else {
                    // Extract protocol for error message
                    val protocolEnd = cleanUrl.indexOf("://")
                    val protocol = if (protocolEnd > 0) cleanUrl.substring(0, protocolEnd + 3) else "unknown"
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
                        // Clean up the token
                        val cleanToken = token.trimEnd('.', ',', ')', ']', '}', '!', '?', ';', ':')

                        // Check protocol
                        if (cleanToken.startsWith("http://") || cleanToken.startsWith("https://")) {
                            if (isValidHttpUrl(cleanToken)) {
                                val url = normalizeUrl(cleanToken)
                                validUrls.add(url)
                            } else {
                                errors.add("Malformed URL detected: \"$cleanToken\".")
                            }
                        } else {
                            errors.add("Unsupported protocol in URL: \"$cleanToken\". Only http and https are supported.")
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
     * Validates if a URL is a properly formed HTTP/HTTPS URL
     */
    private fun isValidHttpUrl(url: String): Boolean {
        try {
            // Basic structure validation
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false
            }

            // Check for minimum required parts: protocol + host
            val withoutProtocol = if (url.startsWith("https://")) {
                url.substring(8) // Remove "https://"
            } else {
                url.substring(7) // Remove "http://"
            }

            // Must have at least a host part
            if (withoutProtocol.isEmpty() || withoutProtocol.startsWith("/")) {
                return false
            }

            // Split into host and path parts
            val parts = withoutProtocol.split("/", limit = 2)
            val hostPart = parts[0]

            // Host cannot be empty
            if (hostPart.isEmpty()) {
                return false
            }

            // Basic host validation
            if (hostPart.contains("..") || hostPart.startsWith(".") || hostPart.endsWith(".")) {
                return false
            }

            // Check for invalid characters in host
            if (hostPart.contains(" ")) {
                return false
            }

            // Check for malformed IPv6 addresses (must be properly bracketed)
            if (hostPart.contains("[") || hostPart.contains("]")) {
                // IPv6 addresses must be fully enclosed in brackets
                if (!hostPart.startsWith("[") || !hostPart.endsWith("]")) {
                    return false
                }
                // Basic check for IPv6 format (simplified)
                val ipv6Content = hostPart.substring(1, hostPart.length - 1)
                if (ipv6Content.isEmpty() || ipv6Content.contains("[") || ipv6Content.contains("]")) {
                    return false
                }
            }

            // Check for valid port if present
            if (hostPart.contains(":")) {
                val hostAndPort = hostPart.split(":")
                if (hostAndPort.size != 2) return false

                val portStr = hostAndPort[1]
                if (portStr.isEmpty()) return false

                try {
                    val port = portStr.toInt()
                    if (port < 1 || port > 65535) return false
                } catch (e: NumberFormatException) {
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            return false
        }
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
            val result = StringBuilder()
            try {
                llmService.streamPrompt(
                    userPrompt = fallbackPrompt,
                    compileDevIns = false
                ).collect { chunk ->
                    result.append(chunk)
                }
            } catch (e: Exception) {
                // If streaming fails, we still want to return what we collected so far
                if (result.isEmpty()) {
                    throw ToolException(
                        "Failed to process content with LLM: ${e.message}",
                        ToolErrorType.WEB_FETCH_PROCESSING_ERROR
                    )
                }
                // If we have some content, log the error but continue
                println("Warning: LLM streaming was interrupted but partial content was collected: ${e.message}")
            }

            return ToolResult.Success(
                content = result.toString(),
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

