package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.tool.ExecutableTool

/**
 * Initializer for MCP tools in the CodingAgent
 * 
 * Handles the lifecycle of MCP client connections and tool discovery:
 * 1. Load MCP configuration from ConfigManager
 * 2. Initialize MCP client manager
 * 3. Discover available tools from configured servers
 * 4. Create tool adapters for integration with the agent system
 */
class McpToolsInitializer {
    private var clientManager: McpClientManager? = null
    private val discoveredTools = mutableMapOf<String, List<McpToolInfo>>()
    
    /**
     * Initialize MCP tools from configuration
     * 
     * @param mcpServers Map of server name to server configuration
     * @return List of executable tools ready to be registered with the agent
     */
    suspend fun initialize(mcpServers: Map<String, McpServerConfig>): List<ExecutableTool<*, *>> {
        if (mcpServers.isEmpty()) {
            return emptyList()
        }
        
        try {
            // Create MCP client manager
            clientManager = McpClientManagerFactory.create()
            
            // Initialize with configuration
            val mcpConfig = McpConfig(mcpServers = mcpServers)
            clientManager?.initialize(mcpConfig)
            
            // Discover tools from all servers
            val tools = clientManager?.discoverAllTools() ?: emptyMap()
            
            // Store discovered tools
            discoveredTools.clear()
            discoveredTools.putAll(tools)
            
            // Enable all discovered tools by default
            val enabledTools = tools.mapValues { (_, toolList) ->
                toolList.map { it.copy(enabled = true) }
            }
            
            // Create tool adapters
            return McpToolAdapterFactory.createAdapters(enabledTools, clientManager!!)
        } catch (e: Exception) {
            println("Error initializing MCP tools: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
    
    /**
     * Get all discovered tools (for UI display)
     */
    fun getDiscoveredTools(): Map<String, List<McpToolInfo>> {
        return discoveredTools.toMap()
    }
    
    /**
     * Get server statuses
     */
    fun getServerStatuses(): Map<String, McpServerStatus> {
        return clientManager?.getAllServerStatuses() ?: emptyMap()
    }
    
    /**
     * Shutdown and clean up MCP connections
     */
    suspend fun shutdown() {
        try {
            clientManager?.shutdown()
        } catch (e: Exception) {
            println("Error shutting down MCP client manager: ${e.message}")
        } finally {
            clientManager = null
            discoveredTools.clear()
        }
    }
    
    /**
     * Check if MCP tools are initialized
     */
    fun isInitialized(): Boolean {
        return clientManager != null && 
               clientManager?.getDiscoveryState() == McpDiscoveryState.COMPLETED
    }
}

