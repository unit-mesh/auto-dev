package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.FetchResult
import cc.unitmesh.agent.tool.impl.HttpFetcher
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Ktor-based HTTP fetcher implementation for all platforms
 * 
 * This implementation uses Ktor HTTP client which supports:
 * - JVM (CIO, Apache, OkHttp, Java)
 * - JS (Browser fetch API, node-fetch)
 * - Native (Darwin, Curl, WinHttp)
 */
class KtorHttpFetcher(
    private val httpClient: HttpClient
) : HttpFetcher {
    
    override suspend fun fetch(url: String, timeout: Long): FetchResult {
        return try {
            withTimeout(timeout) {
                val response: HttpResponse = httpClient.get(url) {
                    header(HttpHeaders.UserAgent, "Mozilla/5.0 (compatible; AutoDev/1.0)")
                }

                val statusCode = response.status.value
                
                if (statusCode !in 200..299) {
                    return@withTimeout FetchResult(
                        success = false,
                        content = "",
                        statusCode = statusCode,
                        error = "HTTP $statusCode: ${response.status.description}"
                    )
                }

                val contentType = response.contentType()?.toString() ?: ""
                val rawContent = response.bodyAsText()

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
            }
        } catch (e: TimeoutCancellationException) {
            FetchResult(
                success = false,
                content = "",
                error = "Request timed out after ${timeout}ms"
            )
        } catch (e: Exception) {
            FetchResult(
                success = false,
                content = "",
                error = "Error fetching URL: ${e.message}"
            )
        }
    }

    /**
     * Simple HTML to text conversion
     * Removes script, style tags and HTML tags
     */
    private fun convertHtmlToText(html: String): String {
        var text = html
        
        // Remove script and style tags with their content
        text = text.replace(Regex("<script\\b[^<]*(?:(?!</script>)<[^<]*)*</script>", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("<style\\b[^<]*(?:(?!</style>)<[^<]*)*</style>", RegexOption.IGNORE_CASE), "")
        
        // Remove HTML tags
        text = text.replace(Regex("<[^>]+>"), " ")
        
        // Decode common HTML entities
        text = text
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
        
        // Normalize whitespace
        text = text.replace(Regex("\\s+"), " ").trim()
        
        return text
    }
    
    companion object {
        /**
         * Create a default KtorHttpFetcher with appropriate engine for the platform
         * 
         * Uses expect/actual pattern via HttpClientFactory to select:
         * - JVM: CIO engine
         * - JS: Js engine (fetch API)
         * - Native: Platform-specific engines
         */
        fun create(): KtorHttpFetcher {
            val client = HttpClientFactory.create()
            return KtorHttpFetcher(client)
        }
    }
}

