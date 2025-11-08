package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.FetchResult
import cc.unitmesh.agent.tool.impl.HttpFetcher
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/**
 * Node.js-specific HTTP fetcher using native fetch API
 * 
 * This implementation bypasses Ktor's JS engine limitations in Node.js
 * and directly uses Node.js's built-in fetch API (available in Node.js 18+)
 */
class NodeFetchHttpFetcher : HttpFetcher {
    
    override suspend fun fetch(url: String, timeout: Long): FetchResult {
        return try {
            val response = fetchWithTimeout(url, timeout)
            
            val statusCode = response.status.toInt()
            
            if (statusCode !in 200..299) {
                return FetchResult(
                    success = false,
                    content = "",
                    statusCode = statusCode,
                    error = "HTTP $statusCode: ${response.statusText}"
                )
            }
            
            val contentType = getHeaderValue(response.headers, "content-type") ?: ""
            val rawContent = response.text().await()
            
            // Simple HTML to text conversion if content is HTML
            val textContent = if (contentType.contains("text/html", ignoreCase = true)) {
                convertHtmlToText(rawContent)
            } else {
                rawContent
            }
            
            FetchResult(
                success = true,
                content = textContent,
                contentType = contentType,
                statusCode = statusCode
            )
        } catch (e: Throwable) {
            val errorMessage = when {
                e.message?.contains("abort", ignoreCase = true) == true -> "Request timeout after ${timeout}ms"
                e.message?.contains("fetch", ignoreCase = true) == true -> "Network error: ${e.message}"
                else -> e.message ?: "Unknown error"
            }
            
            FetchResult(
                success = false,
                content = "",
                statusCode = 0,
                error = errorMessage
            )
        }
    }
    
    /**
     * Fetch with timeout using AbortController
     */
    private suspend fun fetchWithTimeout(url: String, timeout: Long): Response {
        return suspendCancellableCoroutine { continuation ->
            val controller = AbortController()
            
            var timeoutHandle: dynamic = null
            timeoutHandle = setTimeout({
                controller.abort()
            }, timeout.toInt())
            
            fetch(url, FetchOptions(
                signal = controller.signal,
                headers = js("({ 'User-Agent': 'Mozilla/5.0 (compatible; AutoDev/1.0)' })")
            )).then(
                onFulfilled = { response ->
                    clearTimeout(timeoutHandle)
                    continuation.resume(response)
                },
                onRejected = { error ->
                    clearTimeout(timeoutHandle)
                    continuation.resumeWithException(Exception(error.toString()))
                }
            )
        }
    }
    
    /**
     * Get header value
     */
    private fun getHeaderValue(headers: Headers, name: String): String? {
        val value = headers.get(name)
        return value.takeIf { it != null && it != js("undefined") }
    }
    
    /**
     * Simple HTML to text conversion
     */
    private fun convertHtmlToText(html: String): String {
        var text = html
        
        // Remove script and style tags with their content
        text = text.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style[^>]*>.*?</style>", RegexOption.IGNORE_CASE), "")
        
        // Remove HTML tags
        text = text.replace(Regex("<[^>]+>"), " ")
        
        // Decode common HTML entities
        text = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        
        // Normalize whitespace
        text = text.replace(Regex("\\s+"), " ").trim()
        
        return text
    }
}

// External declarations for Node.js fetch API (global functions)
external fun fetch(url: String, options: FetchOptions = definedExternally): Promise<Response>

external fun setTimeout(handler: () -> Unit, timeout: Int): dynamic

external fun clearTimeout(handle: dynamic)

external class AbortController {
    val signal: AbortSignal
    fun abort()
}

external interface AbortSignal

external class Response {
    val status: Number
    val statusText: String
    val headers: Headers
    fun text(): Promise<String>
}

external class Headers {
    fun get(name: String): String?
}

external interface FetchOptions {
    var signal: AbortSignal?
    var headers: dynamic
}

fun FetchOptions(signal: AbortSignal? = null, headers: dynamic = null): FetchOptions {
    val options = js("{}").unsafeCast<FetchOptions>()
    if (signal != null) options.signal = signal
    if (headers != null) options.headers = headers
    return options
}
