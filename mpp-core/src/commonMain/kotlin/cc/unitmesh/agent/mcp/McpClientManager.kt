package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.Tool

/**
 * MCP Client Manager - Manages connections to MCP servers and tool discovery
 * 
 * This is an expect/actual interface to support different platforms:
 * - JVM: Uses io.modelcontextprotocol:kotlin-sdk
 * - JS: Uses @modelcontextprotocol/sdk (TypeScript SDK)
 * - Other platforms: Stub implementation
 * 
 * Based on:
 * - AutoDev IDEA: core/src/main/kotlin/cc/unitmesh/devti/mcp/client/CustomMcpServerManager.kt
 * - Gemini CLI: Samples/gemini-cli/packages/core/src/tools/mcp-client-manager.ts
 */
expect class McpClientManager {
    /**
     * Initialize the manager with MCP configuration
     */
    suspend fun initialize(config: McpConfig)
    
    /**
     * Discover all tools from all configured MCP servers
     * Returns a map of server name to list of discovered tools
     */
    suspend fun discoverAllTools(): Map<String, List<McpToolInfo>>

    /**
     * Discover tools from a specific MCP server
     * Returns a list of discovered tools for the specified server
     */
    suspend fun discoverServerTools(serverName: String): List<McpToolInfo>
    
    /**
     * Get the current status of a specific MCP server
     */
    fun getServerStatus(serverName: String): McpServerStatus
    
    /**
     * Get all server statuses
     */
    fun getAllServerStatuses(): Map<String, McpServerStatus>
    
    /**
     * Execute a tool on a specific MCP server
     * 
     * @param serverName The name of the MCP server
     * @param toolName The name of the tool to execute
     * @param arguments JSON string of tool arguments
     * @return The result of the tool execution as a JSON string
     */
    suspend fun executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ): String
    
    /**
     * Disconnect from all MCP servers and clean up resources
     */
    suspend fun shutdown()
    
    /**
     * Get discovery state
     */
    fun getDiscoveryState(): McpDiscoveryState
}

/**
 * Factory for creating platform-specific McpClientManager instances
 */
expect object McpClientManagerFactory {
    /**
     * Create a new McpClientManager instance
     */
    fun create(): McpClientManager
}

