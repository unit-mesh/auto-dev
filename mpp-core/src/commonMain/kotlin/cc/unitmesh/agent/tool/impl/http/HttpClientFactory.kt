package cc.unitmesh.agent.tool.impl.http

import io.ktor.client.*

/**
 * Platform-specific HttpClient factory using expect/actual pattern
 * 
 * Each platform provides its own optimal HTTP client engine:
 * - JVM: CIO engine (fully asynchronous, coroutine-based)
 * - JS: JS engine (uses fetch API)
 * - Native: Platform-specific engines (Darwin, Curl, WinHttp)
 */
expect object HttpClientFactory {
    /**
     * Create a platform-specific HttpClient instance
     * 
     * @return HttpClient configured for the current platform
     */
    fun create(): HttpClient
}

