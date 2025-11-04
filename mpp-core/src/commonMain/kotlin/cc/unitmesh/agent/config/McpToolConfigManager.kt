package cc.unitmesh.agent.config

import cc.unitmesh.agent.mcp.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

/**
 * MCP Tool Configuration Manager
 *
 * Manages MCP server configurations and tool discovery for mpp-core.
 * This is the mpp-core equivalent of CustomMcpServerManager from the core module.
 *
 * Based on:
 * - AutoDev IDEA: core/src/main/kotlin/cc/unitmesh/devti/mcp/client/CustomMcpServerManager.kt
 * - MCP Kotlin SDK: https://github.com/modelcontextprotocol/kotlin-sdk
 */
object McpToolConfigManager {
    private val clientManager: McpClientManager by lazy { McpClientManagerFactory.create() }
    private val cached = mutableMapOf<String, Map<String, List<ToolItem>>>()
    private val loadingStateCallbacks = mutableListOf<McpLoadingStateCallback>()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Discover MCP tools from server configurations
     *
     * @param mcpServers Map of server name to server configuration
     * @param enabledMcpTools Set of enabled MCP tool names
     * @return Map of server name to list of discovered MCP tools as ToolItems
     */
    suspend fun discoverMcpTools(
        mcpServers: Map<String, McpServerConfig>,
        enabledMcpTools: Set<String>
    ): Map<String, List<ToolItem>> {
        if (mcpServers.isEmpty()) return emptyMap()

        // Create cache key from server configurations
        val cacheKey = createCacheKey(mcpServers)

        // Check cache first
        cached[cacheKey]?.let { cachedTools ->
            return applyEnabledState(cachedTools, enabledMcpTools)
        }

        try {
            // Initialize MCP client manager with configuration
            val mcpConfig = McpConfig(mcpServers = mcpServers)
            clientManager.initialize(mcpConfig)

            // Discover tools from all servers
            val discoveredTools = clientManager.discoverAllTools()

            // Convert to ToolItem format and cache
            val toolsByServer = convertMcpToolsToToolItems(discoveredTools)
            cached[cacheKey] = toolsByServer

            return applyEnabledState(toolsByServer, enabledMcpTools)
        } catch (e: Exception) {
            println("Error discovering MCP tools: ${e.message}")
            e.printStackTrace()
            return emptyMap()
        }
    }

    /**
     * Discover MCP tools with incremental loading support
     *
     * @param mcpServers Map of server name to server configuration
     * @param enabledMcpTools Set of enabled MCP tool names
     * @param callback Callback for loading state updates
     * @return Initial loading state with server states
     */
    suspend fun discoverMcpToolsIncremental(
        mcpServers: Map<String, McpServerConfig>,
        enabledMcpTools: Set<String>,
        callback: McpLoadingStateCallback
    ): McpLoadingState {
        if (mcpServers.isEmpty()) return McpLoadingState()

        // Initialize loading state with all servers
        var loadingState = McpLoadingState()

        // Set initial server states
        mcpServers.forEach { (serverName, config) ->
            val status = if (config.disabled) {
                McpServerLoadingStatus.DISABLED
            } else {
                McpServerLoadingStatus.NOT_STARTED
            }
            loadingState = loadingState.updateServerStatus(serverName, status)
        }

        // Notify initial state
        callback.onLoadingStateChanged(loadingState)

        try {
            // Initialize MCP client manager
            val mcpConfig = McpConfig(mcpServers = mcpServers)
            clientManager.initialize(mcpConfig)

            // Launch concurrent loading for each enabled server
            coroutineScope {
                val jobs = mcpServers.filter { !it.value.disabled }.map { (serverName, _) ->
                    async {
                        loadServerToolsAsync(serverName, enabledMcpTools, callback)
                    }
                }

                // Wait for all servers to complete (but don't block UI)
                jobs.awaitAll()
            }


        } catch (e: Exception) {
            println("Error initializing MCP client manager: ${e.message}")
            e.printStackTrace()
        }

        return loadingState
    }

    /**
     * Load tools for a single server asynchronously
     */
    private suspend fun loadServerToolsAsync(
        serverName: String,
        enabledMcpTools: Set<String>,
        callback: McpLoadingStateCallback
    ) {
        try {
            // Update status to loading
            val loadingState = McpServerState(
                serverName = serverName,
                status = McpServerLoadingStatus.LOADING,
                loadingStartTime = getCurrentTimeMillis()
            )
            callback.onServerStateChanged(serverName, loadingState)

            // Discover tools for this server
            val discoveredTools = clientManager.discoverServerTools(serverName)

            // Convert to ToolItem format
            val tools = discoveredTools.map { toolInfo ->
                ToolItem(
                    name = "${serverName}_${toolInfo.name}",
                    displayName = toolInfo.name,
                    description = toolInfo.description,
                    category = "MCP",
                    source = ToolSource.MCP,
                    enabled = "${serverName}_${toolInfo.name}" in enabledMcpTools,
                    serverName = serverName
                )
            }

            // Update status to loaded
            val loadedState = McpServerState(
                serverName = serverName,
                status = McpServerLoadingStatus.LOADED,
                tools = tools,
                loadingStartTime = loadingState.loadingStartTime,
                loadingEndTime = getCurrentTimeMillis()
            )
            callback.onServerStateChanged(serverName, loadedState)

        } catch (e: Exception) {
            println("Error loading tools for server '$serverName': ${e.message}")

            // Update status to error
            val errorState = McpServerState(
                serverName = serverName,
                status = McpServerLoadingStatus.ERROR,
                errorMessage = e.message,
                loadingStartTime = getCurrentTimeMillis(),
                loadingEndTime = getCurrentTimeMillis()
            )
            callback.onServerStateChanged(serverName, errorState)
        }
    }

    /**
     * Register a callback for loading state updates
     */
    fun addLoadingStateCallback(callback: McpLoadingStateCallback) {
        loadingStateCallbacks.add(callback)
    }

    /**
     * Unregister a callback for loading state updates
     */
    fun removeLoadingStateCallback(callback: McpLoadingStateCallback) {
        loadingStateCallbacks.remove(callback)
    }

    /**
     * Get enabled servers from configuration string
     *
     * @param configContent JSON configuration string
     * @return Map of enabled server configurations
     */
    fun getEnabledServers(configContent: String): Map<String, McpServerConfig>? {
        return try {
            val mcpConfig = McpConfig.fromJson(configContent)
            mcpConfig?.getEnabledServers()
        } catch (e: Exception) {
            println("Error parsing MCP configuration: ${e.message}")
            null
        }
    }

    /**
     * Execute an MCP tool
     *
     * @param serverName Name of the MCP server
     * @param toolName Name of the tool to execute
     * @param arguments JSON string of tool arguments
     * @return Tool execution result
     */
    suspend fun executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ): String {
        return try {
            clientManager.executeTool(serverName, toolName, arguments)
        } catch (e: Exception) {
            "Error executing tool '$toolName' on server '$serverName': ${e.message}"
        }
    }

    /**
     * Get server connection statuses
     *
     * @return Map of server name to connection status
     */
    fun getServerStatuses(): Map<String, McpServerStatus> {
        return clientManager.getAllServerStatuses()
    }

    /**
     * Shutdown MCP connections and clean up resources
     */
    suspend fun shutdown() {
        try {
            clientManager.shutdown()
            cached.clear()
        } catch (e: Exception) {
            println("Error shutting down MCP client manager: ${e.message}")
        }
    }

    /**
     * Clear cached tool discoveries
     */
    fun clearCache() {
        cached.clear()
    }

    // Private helper methods

    private fun createCacheKey(mcpServers: Map<String, McpServerConfig>): String {
        return json.encodeToString(McpConfig.serializer(), McpConfig(mcpServers))
    }

    private fun convertMcpToolsToToolItems(
        discoveredTools: Map<String, List<McpToolInfo>>
    ): Map<String, List<ToolItem>> {
        return discoveredTools.mapValues { (serverName, tools) ->
            tools.map { toolInfo ->
                ToolItem(
                    name = "${serverName}_${toolInfo.name}",
                    displayName = toolInfo.name,
                    description = toolInfo.description,
                    category = "MCP",
                    source = ToolSource.MCP,
                    enabled = toolInfo.enabled,
                    serverName = serverName
                )
            }
        }
    }

    private fun applyEnabledState(
        toolsByServer: Map<String, List<ToolItem>>,
        enabledMcpTools: Set<String>
    ): Map<String, List<ToolItem>> {
        return toolsByServer.mapValues { (_, tools) ->
            tools.map { toolItem ->
                toolItem.copy(enabled = toolItem.name in enabledMcpTools)
            }
        }
    }
}
