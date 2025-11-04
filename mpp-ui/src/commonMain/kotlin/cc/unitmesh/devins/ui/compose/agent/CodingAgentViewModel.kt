package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.PreloadingStatus
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

            // Final status update
            mcpPreloadingStatus = McpToolConfigManager.getPreloadingStatus()
            val preloadedCount = mcpPreloadingStatus.preloadedServers.size
            val totalCount = toolConfig.mcpServers.filter { !it.value.disabled }.size

            mcpPreloadingMessage = if (preloadedCount > 0) {
                "MCP servers loaded successfully ($preloadedCount/$totalCount servers)"
            } else {
                "MCP servers initialization completed (no tools loaded)"
            }

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
     * Get tool loading status for UI display
     */
    fun getToolLoadingStatus(): ToolLoadingStatus {
        return ToolLoadingStatus(
            builtinToolsEnabled = 0, // Assume all built-in tools are enabled by default
            builtinToolsTotal = 5, // read-file, write-file, grep, glob, shell
            subAgentsEnabled = 3, // error-recovery, log-summary, codebase-investigator
            subAgentsTotal = 3,
            mcpServersLoaded = mcpPreloadingStatus.preloadedServers.size,
            mcpServersTotal = 2, // filesystem, context7 (hardcoded for now)
            mcpToolsEnabled = mcpPreloadingStatus.totalCachedConfigurations * 14, // Estimate 14 tools per server
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
