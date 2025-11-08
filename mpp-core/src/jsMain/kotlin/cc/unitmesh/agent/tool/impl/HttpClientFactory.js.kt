package cc.unitmesh.agent.tool.impl.http

import io.ktor.client.*
import io.ktor.client.engine.js.*

/**
 * JavaScript implementation of HttpClientFactory using Js engine
 * 
 * Js engine uses:
 * - Browser fetch API for browser environments
 * - node-fetch for Node.js environments
 * - Supports HTTP/2
 * - WebSocket support
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Js) {
            expectSuccess = false // Don't throw on non-2xx responses
            
            // JS engine doesn't require much configuration
            // It automatically uses the appropriate fetch API
        }
    }
}

