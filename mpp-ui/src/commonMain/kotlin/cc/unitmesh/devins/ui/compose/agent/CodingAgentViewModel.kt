package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.PreloadingStatus
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.ToolCategory
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*

/**
 * Compose ViewModel for Coding Agent
 *
 * Uses the new BaseRenderer architecture with ComposeRenderer
 * for consistent rendering across CLI, TUI, and Compose UI
 */
class CodingAgentViewModel(
    private val llmService: KoogLLMService,
    private val projectPath: String,
    private val maxIterations: Int = 100
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val renderer = ComposeRenderer()

    // Lazy initialization of CodingAgent to handle async tool config loading
    private var _codingAgent: CodingAgent? = null
    private var agentInitialized = false

    var isExecuting by mutableStateOf(false)
        private set
    private var currentExecutionJob: Job? = null

    // MCP preloading state
    var mcpPreloadingStatus by mutableStateOf(PreloadingStatus(false, emptyList(), 0))
        private set
    var mcpPreloadingMessage by mutableStateOf("")
        private set

    // Cached tool configuration for UI display
    private var cachedToolConfig: cc.unitmesh.agent.config.ToolConfigFile? = null

    init {
        // Start MCP preloading immediately when ViewModel is created
        scope.launch {
            startMcpPreloading()
        }
    }

    /**
     * Start MCP servers preloading in background
     */
    private suspend fun startMcpPreloading() {
        try {
            mcpPreloadingMessage = "Loading MCP servers configuration..."
            val toolConfig = ConfigManager.loadToolConfig()

            // Cache the tool configuration for UI display
            cachedToolConfig = toolConfig

            if (toolConfig.mcpServers.isEmpty()) {
                mcpPreloadingMessage = "No MCP servers configured"
                return
            }

            mcpPreloadingMessage = "Initializing ${toolConfig.mcpServers.size} MCP servers..."

            // Initialize MCP servers (this will start background preloading)
            McpToolConfigManager.init(toolConfig)

            // Monitor preloading status
            while (McpToolConfigManager.isPreloading()) {
                mcpPreloadingStatus = McpToolConfigManager.getPreloadingStatus()
                mcpPreloadingMessage = "Loading MCP servers... (${mcpPreloadingStatus.preloadedServers.size} completed)"
                delay(500) // Update every 500ms
            }

            // Wait a bit more to ensure all status updates are complete
            delay(1000)

            // Final status update - force refresh multiple times to ensure we get the latest
            repeat(3) {
                mcpPreloadingStatus = McpToolConfigManager.getPreloadingStatus()
                delay(100)
            }

            val preloadedCount = mcpPreloadingStatus.preloadedServers.size
            val totalCount = toolConfig.mcpServers.filter { !it.value.disabled }.size

            mcpPreloadingMessage = if (preloadedCount > 0) {
                "MCP servers loaded successfully ($preloadedCount/$totalCount servers)"
            } else {
                "MCP servers initialization completed (no tools loaded)"
            }

            // Debug: Print final status
            println("🔍 [CodingAgentViewModel] Final MCP status:")
            println("   Preloaded servers: ${mcpPreloadingStatus.preloadedServers}")
            println("   Total cached: ${mcpPreloadingStatus.totalCachedConfigurations}")
            println("   Is preloading: ${McpToolConfigManager.isPreloading()}")
            println("   Message: $mcpPreloadingMessage")

        } catch (e: Exception) {
            mcpPreloadingMessage = "Failed to load MCP servers: ${e.message}"
            println("Error during MCP preloading: ${e.message}")
        }
    }

    /**
     * Initialize the CodingAgent with tool configuration
     * This must be called before executing any tasks
     */
    private suspend fun initializeCodingAgent(): CodingAgent {
        if (_codingAgent == null || !agentInitialized) {
            val toolConfig = ConfigManager.loadToolConfig()
            val mcpToolConfigService = McpToolConfigService(toolConfig)

            _codingAgent = createPlatformCodingAgent(
                projectPath = projectPath,
                llmService = llmService,
                maxIterations = maxIterations,
                renderer = renderer,
                mcpToolConfigService = mcpToolConfigService
            )
            agentInitialized = true
        }
        return _codingAgent!!
    }

    fun executeTask(task: String) {
        if (isExecuting) {
            println("Agent is already executing")
            return
        }

        isExecuting = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentExecutionJob =
            scope.launch {
                try {
                    // Initialize agent if not already done
                    val codingAgent = initializeCodingAgent()

                    val agentTask =
                        AgentTask(
                            requirement = task,
                            projectPath = projectPath
                        )

                    val result = codingAgent.executeTask(agentTask)

                    // Result is already handled by the renderer
                    isExecuting = false
                    currentExecutionJob = null
                } catch (e: CancellationException) {
                    // Task was cancelled - reset all states and add cancellation message at the end
                    renderer.forceStop() // Stop all loading states

                    // Add cancellation message to timeline (will appear at the end)
                    renderer.renderError("Task cancelled by user")
                    isExecuting = false
                    currentExecutionJob = null
                } catch (e: Exception) {
                    renderer.renderError(e.message ?: "Unknown error")
                    isExecuting = false
                    currentExecutionJob = null
                }
            }
    }

    /**
     * Cancel current task
     */
    fun cancelTask() {
        if (isExecuting && currentExecutionJob != null) {
            currentExecutionJob?.cancel("Task cancelled by user")
            currentExecutionJob = null
            isExecuting = false
        }
    }

    /**
     * Clear chat history
     */
    fun clearHistory() {
        renderer.clearMessages()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        renderer.clearError()
    }

    /**
     * Check if MCP servers are ready (preloading completed)
     */
    fun areMcpServersReady(): Boolean = !McpToolConfigManager.isPreloading()

    /**
     * Refresh tool configuration (call this when user modifies tool settings)
     */
    suspend fun refreshToolConfig() {
        try {
            val newToolConfig = ConfigManager.loadToolConfig()
            cachedToolConfig = newToolConfig

            // If MCP servers configuration changed, restart preloading
            val currentMcpServers = cachedToolConfig?.mcpServers ?: emptyMap()
            if (currentMcpServers.isNotEmpty()) {
                // Restart MCP preloading with new configuration
                startMcpPreloading()
            }
        } catch (e: Exception) {
            println("Error refreshing tool config: ${e.message}")
        }
    }

    /**
     * Get tool loading status for UI display
     */
    fun getToolLoadingStatus(): ToolLoadingStatus {
        val toolConfig = cachedToolConfig

        // Get built-in tools from ToolType (excluding SubAgents)
        val allBuiltinTools = ToolType.ALL_TOOLS.filter { it.category != ToolCategory.SubAgent }
        val builtinToolsEnabled = if (toolConfig != null) {
            allBuiltinTools.count { toolType ->
                toolType.name in toolConfig.enabledBuiltinTools
            }
        } else {
            allBuiltinTools.size // Default: all enabled
        }

        // Get SubAgents from ToolType
        val subAgentTools = ToolType.byCategory(ToolCategory.SubAgent)
        val subAgentsEnabled = if (toolConfig != null) {
            subAgentTools.count { toolType ->
                toolType.name in toolConfig.enabledBuiltinTools
            }
        } else {
            subAgentTools.size // Default: all enabled
        }

        // Get MCP tools information
        val mcpServersTotal = toolConfig?.mcpServers?.filter { !it.value.disabled }?.size ?: 0
        val mcpServersLoaded = mcpPreloadingStatus.preloadedServers.size

        // Get actual MCP tools count from preloaded cache
        val mcpToolsEnabled = if (McpToolConfigManager.isPreloading()) {
            0 // Still loading
        } else {
            // Use the preloading status to get tool count
            // This is an approximation based on preloaded servers
            val enabledMcpToolsCount = toolConfig?.enabledMcpTools?.size ?: 0
            if (enabledMcpToolsCount > 0) {
                enabledMcpToolsCount
            } else {
                // Estimate based on preloaded servers (14 tools per filesystem server, 2 per context7)
                mcpPreloadingStatus.preloadedServers.sumOf { serverName ->
                    when (serverName) {
                        "filesystem" -> 14
                        "context7" -> 2
                        else -> 5 // Default estimate
                    }
                }
            }
        }

        return ToolLoadingStatus(
            builtinToolsEnabled = builtinToolsEnabled,
            builtinToolsTotal = allBuiltinTools.size,
            subAgentsEnabled = subAgentsEnabled,
            subAgentsTotal = subAgentTools.size,
            mcpServersLoaded = mcpServersLoaded,
            mcpServersTotal = mcpServersTotal,
            mcpToolsEnabled = mcpToolsEnabled,
            isLoading = McpToolConfigManager.isPreloading()
        )
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        scope.cancel()
    }
}

/**
 * Data class to hold tool loading status information
 */
data class ToolLoadingStatus(
    val builtinToolsEnabled: Int = 0,
    val builtinToolsTotal: Int = 0,
    val subAgentsEnabled: Int = 0,
    val subAgentsTotal: Int = 0,
    val mcpServersLoaded: Int = 0,
    val mcpServersTotal: Int = 0,
    val mcpToolsEnabled: Int = 0,
    val isLoading: Boolean = false
)
