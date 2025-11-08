package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.impl.http.KtorHttpFetcher

/**
 * JVM implementation - uses Ktor with CIO engine
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return KtorHttpFetcher.create()
    }
}

