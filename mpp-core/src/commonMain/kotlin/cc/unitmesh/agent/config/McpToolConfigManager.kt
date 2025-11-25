package cc.unitmesh.agent.config

import cc.unitmesh.agent.logging.getLogger
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
    private val logger = getLogger("McpToolConfigManager")
    private val clientManager: McpClientManager by lazy { McpClientManagerFactory.create() }
    private val cached = mutableMapOf<String, Map<String, List<ToolItem>>>()
    private val loadingStateCallbacks = mutableListOf<McpLoadingStateCallback>()

    // Preloading state management
    private var isPreloading = false
    private var preloadingJob: Job? = null
    private var preloadedServers = mutableSetOf<String>()
    private val preloadingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastDiscoveredToolsCount = 0

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

        cached[cacheKey]?.let { cachedTools ->
            return applyEnabledState(cachedTools, enabledMcpTools)
        }

        try {
            val mcpConfig = McpConfig(mcpServers = mcpServers)
            clientManager.initialize(mcpConfig)

            val discoveredTools = clientManager.discoverAllTools()

            val toolsByServer = convertMcpToolsToToolItems(discoveredTools)
            cached[cacheKey] = toolsByServer

            return applyEnabledState(toolsByServer, enabledMcpTools)
        } catch (e: Exception) {
            logger.error(e) { "Error discovering MCP tools: ${e.message}" }
            return emptyMap()
        }
    }

    /**
     * Initialize MCP servers and preload tools in background
     * This method starts preloading all configured MCP servers to avoid delays during actual usage
     *
     * @param toolConfig The tool configuration containing MCP server settings
     */
    fun init(toolConfig: ToolConfigFile) {
        if (toolConfig.mcpServers.isNullOrEmpty()) {
            logger.debug { "No MCP servers configured, skipping initialization" }
            return
        }

        // Cancel any existing preloading job
        preloadingJob?.cancel()

        // Set preloading flag immediately before starting the job
        isPreloading = true
        preloadedServers.clear()

        // Start background preloading
        try {
            preloadingJob = preloadingScope.launch {
                try {
                    logger.info { "Starting MCP servers preloading for ${toolConfig.mcpServers.size} servers..." }

                // Initialize client manager with MCP config
                val mcpConfig = McpConfig(mcpServers = toolConfig.mcpServers)
                clientManager.initialize(mcpConfig)

                // Create cache key for this configuration
                val cacheKey = createCacheKey(toolConfig.mcpServers)

                // Preload tools from all enabled servers concurrently
                val enabledMcpTools = toolConfig.enabledMcpTools.toSet()
                val preloadResults = mutableMapOf<String, List<ToolItem>>()

                coroutineScope {
                    val jobs = toolConfig.mcpServers.filter { !it.value.disabled }.map { (serverName, _) ->
                        async {
                            try {
                                logger.info { "Preloading tools for MCP server: $serverName" }
                                val discoveredTools = clientManager.discoverServerTools(serverName)

                                val tools = discoveredTools.map { toolInfo ->
                                    ToolItem(
                                        name = toolInfo.name, // Use actual tool name, not prefixed
                                        displayName = toolInfo.name,
                                        description = toolInfo.description,
                                        category = "MCP",
                                        source = ToolSource.MCP,
                                        enabled = toolInfo.name in enabledMcpTools, // Check by actual tool name
                                        serverName = serverName,
                                        schema = toolInfo.inputSchema
                                    )
                                }

                                preloadResults[serverName] = tools
                                preloadedServers.add(serverName)
                                logger.info { "Successfully preloaded ${tools.size} tools from MCP server: $serverName" }

                            } catch (e: Exception) {
                                logger.error(e) { "Failed to preload tools from MCP server '$serverName': ${e.message}" }
                            }
                        }
                    }

                    // Wait for all preloading jobs to complete
                    jobs.awaitAll()
                }

                // Cache the preloaded results
                if (preloadResults.isNotEmpty()) {
                    cached[cacheKey] = preloadResults
                    lastDiscoveredToolsCount = preloadResults.values.sumOf { it.size }
                    logger.info { "MCP servers preloading completed. Cached tools from ${preloadedServers.size} servers." }
                } else {
                    lastDiscoveredToolsCount = 0
                    logger.info { "MCP servers preloading completed but no tools were loaded." }
                }

            } catch (e: Exception) {
                logger.error(e) { "Error during MCP servers preloading: ${e.message}" }
                } finally {
                    isPreloading = false
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error starting MCP preloading job: ${e.message}" }
            isPreloading = false
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
                McpServerLoadingStatus.AVAILABLE
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
            logger.error(e) { "Error initializing MCP client manager: ${e.message}" }
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
                    name = toolInfo.name, // Use actual tool name, not prefixed
                    displayName = toolInfo.name,
                    description = toolInfo.description,
                    category = "MCP",
                    source = ToolSource.MCP,
                    enabled = toolInfo.name in enabledMcpTools, // Check by actual tool name
                    serverName = serverName,
                    schema = toolInfo.inputSchema
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
            logger.error(e) { "Error loading tools for server '$serverName': ${e.message}" }

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

    fun getServerStatuses(): Map<String, McpServerStatus> {
        return clientManager.getAllServerStatuses()
    }

    /**
     * Check if MCP servers are currently being preloaded
     */
    fun isPreloading(): Boolean = isPreloading

    /**
     * Get the set of successfully preloaded server names
     */
    fun getPreloadedServers(): Set<String> = preloadedServers.toSet()

    /**
     * Check if a specific server has been preloaded
     */
    fun isServerPreloaded(serverName: String): Boolean = serverName in preloadedServers

    /**
     * Wait for preloading to complete (useful for testing or when you need to ensure preloading is done)
     */
    suspend fun waitForPreloading() {
        preloadingJob?.join()
    }

    /**
     * Get preloading status information
     */
    fun getPreloadingStatus(): PreloadingStatus {
        return PreloadingStatus(
            isPreloading = isPreloading,
            preloadedServers = preloadedServers.toList(),
            totalCachedConfigurations = cached.size
        )
    }

    /**
     * Get the total number of discovered MCP tools across all servers
     */
    fun getTotalDiscoveredTools(): Int {
        // First try to get from cache
        val cachedTotal = cached.values.sumOf { serverToolsMap ->
            serverToolsMap.values.sumOf { toolsList -> toolsList.size }
        }

        // If cache is empty but we have a recorded count, use that
        return if (cachedTotal > 0) {
            cachedTotal
        } else {
            lastDiscoveredToolsCount
        }
    }

    suspend fun shutdown() {
        try {
            // Cancel preloading job if running
            preloadingJob?.cancel()
            preloadingScope.cancel()

            clientManager.shutdown()
            cached.clear()
            preloadedServers.clear()
        } catch (e: Exception) {
            logger.error(e) { "Error shutting down MCP client manager: ${e.message}" }
        }
    }

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
                    serverName = serverName,
                    schema = toolInfo.inputSchema
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

/**
 * Represents the current preloading status of MCP servers
 */
data class PreloadingStatus(
    val isPreloading: Boolean,
    val preloadedServers: List<String>,
    val totalCachedConfigurations: Int
)
