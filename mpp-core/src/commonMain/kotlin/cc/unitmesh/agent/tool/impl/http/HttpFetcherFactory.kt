package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher

/**
 * Platform-specific HttpFetcher factory
 * 
 * This factory creates the appropriate HttpFetcher implementation for each platform:
 * - JVM/Android: KtorHttpFetcher with CIO engine
 * - JS: NodeFetchHttpFetcher (bypasses Ktor's limitations in Node.js)
 */
expect object HttpFetcherFactory {
    /**
     * Create a platform-appropriate HttpFetcher instance
     * 
     * @return HttpFetcher configured for the current platform
     */
    fun create(): HttpFetcher
}

