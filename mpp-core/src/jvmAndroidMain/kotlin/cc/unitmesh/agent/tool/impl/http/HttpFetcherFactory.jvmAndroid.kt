package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher

/**
 * JVM and Android implementation - uses Ktor with CIO engine
 * Both platforms share the same implementation
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return KtorHttpFetcher.create()
    }
}
