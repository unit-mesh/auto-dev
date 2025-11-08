package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher

/**
 * JVM implementation - uses Ktor with CIO engine
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return KtorHttpFetcher.create()
    }
}

