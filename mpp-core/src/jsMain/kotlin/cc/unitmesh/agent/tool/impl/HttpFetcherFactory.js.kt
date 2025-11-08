package cc.unitmesh.agent.tool.impl.http

import cc.unitmesh.agent.tool.impl.HttpFetcher

/**
 * JavaScript implementation - uses native Node.js fetch API
 * 
 * This bypasses Ktor's JS engine limitations in Node.js environment
 */
actual object HttpFetcherFactory {
    actual fun create(): HttpFetcher {
        return NodeFetchHttpFetcher()
    }
}

