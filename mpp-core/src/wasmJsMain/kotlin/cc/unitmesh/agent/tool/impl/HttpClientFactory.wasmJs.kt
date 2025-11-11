package cc.unitmesh.agent.tool.impl.http

import io.ktor.client.*
import io.ktor.client.engine.js.*

/**
 * WebAssembly implementation of HttpClientFactory using Js engine
 * 
 * Note: In WASM-JS, we use the same Js engine as regular JS
 * It will use browser fetch API or node-fetch depending on runtime
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Js) {
            expectSuccess = false // Don't throw on non-2xx responses
            
            // JS engine automatically uses appropriate fetch API
        }
    }
}
