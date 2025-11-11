package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher
import cc.unitmesh.agent.tool.impl.FetchResult
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * WebAssembly implementation - uses Ktor HttpClient with Js engine
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return WasmKtorHttpFetcher()
    }
}

/**
 * WASM implementation using Ktor's Js engine
 */
class WasmKtorHttpFetcher : HttpFetcher {
    private val client = HttpClientFactory.create()
    
    override suspend fun fetch(url: String, timeout: Long): FetchResult {
        return try {
            val response: HttpResponse = client.get(url) {
                // Set timeout if needed
            }
            
            val contentType = response.contentType()?.toString() ?: ""
            
            FetchResult(
                success = response.status.isSuccess(),
                content = response.bodyAsText(),
                contentType = contentType,
                statusCode = response.status.value,
                error = null
            )
        } catch (e: Exception) {
            FetchResult(
                success = false,
                content = "",
                contentType = "",
                statusCode = 0,
                error = e.message ?: "Unknown error"
            )
        }
    }
}
