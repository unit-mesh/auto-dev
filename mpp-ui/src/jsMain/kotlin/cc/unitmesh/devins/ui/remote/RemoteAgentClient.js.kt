package cc.unitmesh.devins.ui.remote

import io.ktor.client.*
import io.ktor.client.engine.js.*

internal actual fun createHttpClient(): HttpClient {
    return HttpClient(Js) {
        expectSuccess = false // Don't throw on non-2xx responses
    }
}

