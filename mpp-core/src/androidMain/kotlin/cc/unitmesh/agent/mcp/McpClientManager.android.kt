package cc.unitmesh.agent.mcp

/**
 * Android implementation of McpClientManager
 *
 * Stub implementation for now - MCP SDK is not available on Android
 */
actual class McpClientManager {
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()

    actual suspend fun initialize(config: McpConfig) {
        println("McpClientManager.initialize() - Android implementation not yet available")
    }

    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> {
        println("McpClientManager.discoverAllTools() - Android implementation not yet available")
        return emptyMap()
    }

    actual fun getServerStatus(serverName: String): McpServerStatus {
        return serverStatuses[serverName] ?: McpServerStatus.DISCONNECTED
    }

    actual fun getAllServerStatuses(): Map<String, McpServerStatus> {
        return serverStatuses.toMap()
    }

    actual suspend fun executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ): String {
        println("McpClientManager.executeTool() - Android implementation not yet available")
        throw UnsupportedOperationException("MCP tool execution not yet implemented for Android")
    }

    actual suspend fun shutdown() {
        println("McpClientManager.shutdown() - Android implementation not yet available")
    }

    actual fun getDiscoveryState(): McpDiscoveryState {
        return discoveryState
    }
}

actual object McpClientManagerFactory {
    actual fun create(): McpClientManager {
        return McpClientManager()
    }
}

