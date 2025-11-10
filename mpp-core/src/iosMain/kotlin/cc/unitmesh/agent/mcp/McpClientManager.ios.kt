package cc.unitmesh.agent.mcp

/**
 * iOS implementation of McpClientManager
 * Stub implementation - MCP SDK is not available on iOS
 */
actual class McpClientManager {
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()

    actual suspend fun initialize(config: McpConfig) {
        println("McpClientManager.initialize() - iOS implementation not yet available")
    }

    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> {
        println("McpClientManager.discoverAllTools() - iOS implementation not yet available")
        return emptyMap()
    }

    actual suspend fun discoverServerTools(serverName: String): List<McpToolInfo> {
        println("McpClientManager.discoverServerTools() - iOS implementation not yet available")
        return emptyList()
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
        println("McpClientManager.executeTool() - iOS implementation not yet available")
        throw UnsupportedOperationException("MCP tool execution not yet implemented for iOS")
    }

    actual suspend fun shutdown() {
        println("McpClientManager.shutdown() - iOS implementation not yet available")
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

