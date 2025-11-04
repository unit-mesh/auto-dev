package cc.unitmesh.agent.mcp

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * External declarations for MCP SDK
 * These map to @modelcontextprotocol/sdk types
 */
@JsModule("@modelcontextprotocol/sdk/client/index.js")
@JsNonModule
external class Client {
    fun listTools(): Promise<ListToolsResult>
    fun callTool(request: CallToolRequest): Promise<CallToolResult>
    fun close(): Promise<Unit>
}

external interface ListToolsResult {
    val tools: Array<Tool>
}

external interface Tool {
    val name: String
    val description: String?
    val inputSchema: dynamic
}

external interface CallToolRequest {
    val name: String
    val arguments: dynamic
}

external interface CallToolResult {
    val content: Array<dynamic>
}

@JsModule("@modelcontextprotocol/sdk/client/stdio.js")
@JsNonModule
external class StdioClientTransport(command: String, args: Array<String>) {
    fun start(): Promise<Unit>
    fun close(): Promise<Unit>
}

/**
 * JS implementation of McpClientManager using @modelcontextprotocol/sdk
 *
 * Note: This is a simplified implementation that works in Node.js environment.
 * Full implementation would require proper transport setup and error handling.
 */
actual class McpClientManager {
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()
    private val clients = mutableMapOf<String, Client>()
    private var currentConfig: McpConfig? = null

    actual suspend fun initialize(config: McpConfig) {
        try {
            currentConfig = config
            console.log("McpClientManager.initialize() - Initializing ${config.mcpServers.size} servers")

            // For JS, we'll mark servers as ready but won't actually connect until needed
            config.mcpServers.forEach { (name, _) ->
                serverStatuses[name] = McpServerStatus.DISCONNECTED
            }
        } catch (e: Throwable) {
            console.error("Failed to initialize MCP client manager", e)
            throw e
        }
    }

    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> {
        discoveryState = McpDiscoveryState.IN_PROGRESS
        val result = mutableMapOf<String, List<McpToolInfo>>()

        try {
            currentConfig?.mcpServers?.forEach { (serverName, serverConfig) ->
                if (serverConfig.disabled) {
                    console.log("Skipping disabled server: $serverName")
                    return@forEach
                }

                try {
                    serverStatuses[serverName] = McpServerStatus.CONNECTING

                    // For stdio transport only (JS environment limitation)
                    if (serverConfig.isStdioTransport()) {
                        console.log("Discovering tools from $serverName (stdio transport not fully supported in browser)")
                        // In a real Node.js environment, we would:
                        // 1. Create StdioClientTransport
                        // 2. Create Client with transport
                        // 3. Call listTools()
                        // 4. Parse results

                        // For now, return empty list
                        result[serverName] = emptyList()
                        serverStatuses[serverName] = McpServerStatus.CONNECTED
                    } else if (serverConfig.isNetworkTransport()) {
                        console.log("Network transport for $serverName not yet implemented")
                        result[serverName] = emptyList()
                        serverStatuses[serverName] = McpServerStatus.DISCONNECTED
                    }
                } catch (e: Throwable) {
                    console.error("Failed to discover tools from $serverName", e)
                    serverStatuses[serverName] = McpServerStatus.DISCONNECTED
                    result[serverName] = emptyList()
                }
            }
        } finally {
            discoveryState = McpDiscoveryState.COMPLETED
        }

        return result
    }

    actual suspend fun discoverServerTools(serverName: String): List<McpToolInfo> {
        val config = currentConfig ?: return emptyList()
        val serverConfig = config.mcpServers[serverName] ?: return emptyList()

        if (serverConfig.disabled) {
            return emptyList()
        }

        try {
            // For now, return empty list as JS implementation is not fully available
            console.log("discoverServerTools for $serverName - JS implementation not yet available")
            return emptyList()
        } catch (e: Throwable) {
            console.error("Error discovering tools for server '$serverName'", e)
            return emptyList()
        }
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
        val client = clients[serverName]
            ?: throw IllegalStateException("Server $serverName is not connected")

        try {
            val request = js("({ name: toolName, arguments: JSON.parse(arguments) })") as CallToolRequest
            val result = client.callTool(request).await()
            return JSON.stringify(result.content)
        } catch (e: Throwable) {
            console.error("Failed to execute tool $toolName on $serverName", e)
            throw e
        }
    }

    actual suspend fun shutdown() {
        try {
            clients.values.forEach { client ->
                try {
                    client.close().await()
                } catch (e: Throwable) {
                    console.error("Error closing client", e)
                }
            }
            clients.clear()
            serverStatuses.clear()
        } catch (e: Throwable) {
            console.error("Error during shutdown", e)
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

