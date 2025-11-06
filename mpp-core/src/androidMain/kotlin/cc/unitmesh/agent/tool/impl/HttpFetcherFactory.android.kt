package cc.unitmesh.agent.tool.impl

/**
 * Android implementation - uses Ktor with CIO engine
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return KtorHttpFetcher.create()
    }
}

