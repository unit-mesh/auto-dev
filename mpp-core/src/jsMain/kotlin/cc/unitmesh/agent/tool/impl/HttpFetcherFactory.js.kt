package cc.unitmesh.agent.tool.impl

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

