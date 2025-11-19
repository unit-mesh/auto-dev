package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.impl.http.HttpFetcherFactory
import cc.unitmesh.agent.tool.impl.http.UrlParser
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.llm.KoogLLMService
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

class WebFetchInvocation(
    params: WebFetchParams,
    tool: WebFetchTool,
    private val llmService: KoogLLMService?,
    private val httpFetcher: HttpFetcher
) : BaseToolInvocation<WebFetchParams, ToolResult>(params, tool) {
    private val logger = getLogger("WebFetchInvocation")

    override fun getDescription(): String {
        val displayPrompt = if (params.prompt.length > 100) {
            params.prompt.take(97) + "..."
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

            executeFallback(parsedUrls.validUrls.first())
        }
    }

    private suspend fun executeFallback(url: String): ToolResult {
        try {
            val fetchResult = httpFetcher.fetch(url, timeout = 10000)

            if (!fetchResult.success) {
                throw ToolException(
                    "Failed to fetch URL: ${fetchResult.error}",
                    ToolErrorType.WEB_FETCH_FAILED
                )
            }

            val content = fetchResult.content.take(100000)
            if (llmService == null) {
                return ToolResult.Success(content)
            }

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
                llmService.streamPrompt(userPrompt = fallbackPrompt, compileDevIns = false)?.collect { chunk ->
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

                logger.warn(e) { "Warning: LLM streaming was interrupted but partial content was collected: ${e.message}" }
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

interface HttpFetcher {
    suspend fun fetch(url: String, timeout: Long = 10000): FetchResult
}

data class FetchResult(
    val success: Boolean,
    val content: String,
    val contentType: String = "",
    val statusCode: Int = 0,
    val error: String? = null
)

class WebFetchTool(
    private val llmService: KoogLLMService? = null
) : BaseExecutableTool<WebFetchParams, ToolResult>() {

    private val httpFetcher: HttpFetcher by lazy {
        HttpFetcherFactory.create()
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

