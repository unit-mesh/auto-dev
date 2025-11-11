package cc.unitmesh.agent.mcp

/**
 * WebAssembly implementation of McpClientManager
 * WASM has limited external JS interop, so we provide a minimal stub implementation
 */
actual class McpClientManager {
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()
    private var currentConfig: McpConfig? = null

    actual suspend fun initialize(config: McpConfig) {
        try {
            currentConfig = config
            println("McpClientManager.initialize() - WASM stub implementation, ${config.mcpServers.size} servers registered")

            // In WASM, we can't actually connect to MCP servers
            // Mark all servers as disconnected
            config.mcpServers.forEach { (name, _) ->
                serverStatuses[name] = McpServerStatus.DISCONNECTED
            }
        } catch (e: Throwable) {
            println("Failed to initialize MCP client manager: ${e.message}")
            throw e
        }
    }

    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> {
        discoveryState = McpDiscoveryState.IN_PROGRESS
        val result = mutableMapOf<String, List<McpToolInfo>>()

        try {
            currentConfig?.mcpServers?.forEach { (serverName, serverConfig) ->
                if (serverConfig.disabled) {
                    println("Skipping disabled server: $serverName")
                    return@forEach
                }

                println("WASM stub: Cannot discover tools from $serverName")
                result[serverName] = emptyList()
                serverStatuses[serverName] = McpServerStatus.DISCONNECTED
            }
        } finally {
            discoveryState = McpDiscoveryState.COMPLETED
        }

        return result
    }

    actual suspend fun discoverServerTools(serverName: String): List<McpToolInfo> {
        println("WASM stub: discoverServerTools for $serverName - not supported")
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
        throw UnsupportedOperationException("Tool execution not supported in WASM environment")
    }

    actual suspend fun shutdown() {
        try {
            serverStatuses.clear()
            println("WASM stub: McpClientManager shutdown")
        } catch (e: Throwable) {
            println("Error during shutdown: ${e.message}")
        }
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
