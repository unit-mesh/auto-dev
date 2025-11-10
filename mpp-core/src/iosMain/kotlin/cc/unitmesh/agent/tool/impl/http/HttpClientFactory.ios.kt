package cc.unitmesh.agent.tool.impl.http

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual object HttpClientFactory {
    actual fun create(): HttpClient {
        return HttpClient(Darwin) {
            engine {
                configureRequest {
                    setAllowsCellularAccess(true)
                }
            }
        }
    }
}

