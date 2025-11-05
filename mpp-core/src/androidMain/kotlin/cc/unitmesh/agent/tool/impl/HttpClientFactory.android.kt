package cc.unitmesh.agent.tool.impl

import io.ktor.client.*
import io.ktor.client.engine.cio.*

/**
 * Android implementation of HttpClientFactory using CIO engine
 * 
 * CIO (Coroutine I/O) engine is:
 * - Fully asynchronous and coroutine-based
 * - Supports HTTP/1.x
 * - Works on JVM, Android, and Native platforms
 * - Lightweight and efficient for most use cases
 */
actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(CIO) {
            expectSuccess = false // Don't throw on non-2xx responses
            
            engine {
                // CIO engine configuration
                maxConnectionsCount = 1000
                endpoint {
                    maxConnectionsPerRoute = 100
                    pipelineMaxSize = 20
                    keepAliveTime = 5000
                    connectTimeout = 5000
                    connectAttempts = 5
                }
            }
        }
    }
}

