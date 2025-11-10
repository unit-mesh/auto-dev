package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher

/**
 * iOS implementation - uses Ktor with Darwin engine
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return KtorHttpFetcher.create()
    }
}

