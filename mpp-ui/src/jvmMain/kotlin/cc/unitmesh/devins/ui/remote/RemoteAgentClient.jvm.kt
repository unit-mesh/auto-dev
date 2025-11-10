package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.*

internal actual fun createHttpClient(): HttpClient {
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

