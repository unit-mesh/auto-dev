package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.sse.*
import kotlin.time.Duration.Companion.seconds

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        install(SSE) {
            reconnectionTime = 30.seconds
            maxReconnectionAttempts = 3
        }

        expectSuccess = false // Don't throw on non-2xx responses

        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
}

