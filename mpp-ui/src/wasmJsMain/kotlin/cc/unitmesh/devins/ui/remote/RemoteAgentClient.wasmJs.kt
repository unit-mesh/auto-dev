package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.engine.js.*

/**
 * WASM implementation of createHttpClient
 * Uses JS engine for HTTP client
 */
actual fun createHttpClient(): HttpClient {
    return HttpClient(Js) {
        // Configure HTTP client for WASM/JS
    }
}

