package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.sse.*
import kotlin.time.Duration.Companion.seconds

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(SSE) {
            reconnectionTime = 30.seconds
            maxReconnectionAttempts = 3
        }

        expectSuccess = false // Don't throw on non-2xx responses

        engine {
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

