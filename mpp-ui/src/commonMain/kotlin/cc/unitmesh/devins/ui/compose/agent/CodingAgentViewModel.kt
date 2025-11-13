package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.PreloadingStatus
import cc.unitmesh.agent.tool.ToolCategory
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.devins.filesystem.DefaultProjectFileSystem
import cc.unitmesh.devins.llm.ChatHistoryManager
import cc.unitmesh.devins.llm.MessageRole
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.indexer.DomainDictGenerator
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*

/**
 * Compose ViewModel for Coding Agent
 *
 * Uses the new BaseRenderer architecture with ComposeRenderer
 * for consistent rendering across CLI, TUI, and Compose UI
 *
 * 支持会话管理：Agent 模式的对话也会保存到 ChatHistoryManager
 */
class CodingAgentViewModel(
    private val llmService: KoogLLMService?,
    private val projectPath: String,
    private val maxIterations: Int = 100,
    private val chatHistoryManager: ChatHistoryManager? = null  // 新增：会话管理
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

    // TreeView state
    var isTreeViewVisible by mutableStateOf(false)

    fun toggleTreeView() {
        isTreeViewVisible = !isTreeViewVisible
    }

    fun closeTreeView() {
        isTreeViewVisible = false
    }

    // Cached tool configuration for UI display
    private var cachedToolConfig: cc.unitmesh.agent.config.ToolConfigFile? = null

    init {
        // Load historical messages from chatHistoryManager
        chatHistoryManager?.let { manager ->
            val messages = manager.getMessages()
            messages.forEach { message ->
                when (message.role) {
                    MessageRole.USER -> renderer.addUserMessage(message.content)
                    MessageRole.ASSISTANT -> {
                        // Add assistant message to renderer
                        renderer.renderLLMResponseStart()
                        renderer.renderLLMResponseChunk(message.content)
                        renderer.renderLLMResponseEnd()
                    }
                    else -> {}
                }
            }
        }
        
        // Start MCP preloading immediately when ViewModel is created
        // Only if llmService is configured
        if (llmService != null) {
            scope.launch {
                startMcpPreloading()
            }
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

            mcpPreloadingMessage =
                if (preloadedCount > 0) {
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
     * @throws IllegalStateException if llmService is not configured
     */
    private suspend fun initializeCodingAgent(): CodingAgent {
        if (llmService == null) {
            throw IllegalStateException("LLM service is not configured")
        }

        if (_codingAgent == null || !agentInitialized) {
            val toolConfig = ConfigManager.loadToolConfig()
            val mcpToolConfigService = McpToolConfigService(toolConfig)

            _codingAgent =
                createPlatformCodingAgent(
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

    /**
     * Check if LLM service is configured
     */
    fun isConfigured(): Boolean = llmService != null

    fun executeTask(task: String, onConfigRequired: (() -> Unit)? = null) {
        if (isExecuting) {
            println("Agent is already executing")
            return
        }

        // Check if LLM service is configured
        if (!isConfigured()) {
            renderer.addUserMessage(task)
            renderer.renderError("⚠️ LLM model is not configured. Please configure your model to continue.")
            onConfigRequired?.invoke()
            return
        }

        // Check if this is a built-in slash command
        if (task.trim().startsWith("/")) {
            handleBuiltinCommand(task.trim(), onConfigRequired)
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

                    // 保存用户消息到会话历史
                    chatHistoryManager?.addUserMessage(task)

                    val agentTask =
                        AgentTask(
                            requirement = task,
                            projectPath = projectPath
                        )

                    val result = codingAgent.executeTask(agentTask)

                    // 保存 Agent 完成消息到会话历史（简化版本）
                    val resultSummary = "Agent task completed: $task"
                    chatHistoryManager?.addAssistantMessage(resultSummary)

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
     * Handle built-in slash commands
     */
    private fun handleBuiltinCommand(command: String, onConfigRequired: (() -> Unit)? = null) {
        val parts = command.substring(1).trim().split("\\s+".toRegex())
        val commandName = parts[0].lowercase()
        val args = parts.drop(1).joinToString(" ")

        renderer.addUserMessage(command)

        when (commandName) {
            "init" -> {
                // /init command requires LLM configuration
                if (!isConfigured()) {
                    renderer.renderError("⚠️ LLM model is not configured. Please configure your model to use /init command.")
                    onConfigRequired?.invoke()
                    return
                }
                handleInitCommand(args)
            }
            "clear" -> {
                renderer.clearMessages()
                chatHistoryManager?.clearCurrentSession()  // 同时清空会话历史
                renderer.renderFinalResult(true, "✅ Chat history cleared", 0)
            }
            "help" -> {
                val helpText =
                    buildString {
                        appendLine("📖 Available Commands:")
                        appendLine("  /init [--force] - Initialize project domain dictionary")
                        appendLine("  /clear - Clear chat history")
                        appendLine("  /help - Show this help message")
                        appendLine("")
                        appendLine("💡 You can also use @ for agents and other DevIns commands")
                    }
                renderer.renderFinalResult(true, helpText, 0)
            }
            else -> {
                // Unknown command, let the agent handle it
                if (!isConfigured()) {
                    renderer.renderError("⚠️ LLM model is not configured. Please configure your model to continue.")
                    onConfigRequired?.invoke()
                    return
                }

                isExecuting = true
                currentExecutionJob =
                    scope.launch {
                        try {
                            val codingAgent = initializeCodingAgent()
                            val agentTask =
                                AgentTask(
                                    requirement = command,
                                    projectPath = projectPath
                                )
                            codingAgent.executeTask(agentTask)
                            isExecuting = false
                            currentExecutionJob = null
                        } catch (e: Exception) {
                            renderer.renderError(e.message ?: "Unknown error")
                            isExecuting = false
                            currentExecutionJob = null
                        }
                    }
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
        chatHistoryManager?.clearCurrentSession()
    }

    /**
     * Create new session and switch to it
     */
    fun newSession() {
        renderer.clearMessages()
        chatHistoryManager?.createSession()
    }
    
    /**
     * Switch to a different session and load its messages
     */
    fun switchSession(sessionId: String) {
        chatHistoryManager?.let { manager ->
            val session = manager.switchSession(sessionId)
            if (session != null) {
                // Clear current renderer state
                renderer.clearMessages()
                
                // Load messages from the switched session
                val messages = manager.getMessages()
                messages.forEach { message ->
                    when (message.role) {
                        MessageRole.USER -> renderer.addUserMessage(message.content)
                        MessageRole.ASSISTANT -> {
                            renderer.renderLLMResponseStart()
                            renderer.renderLLMResponseChunk(message.content)
                            renderer.renderLLMResponseEnd()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Handle /init command for domain dictionary generation
     */
    private fun handleInitCommand(args: String) {
        val force = args.contains("--force")

        scope.launch {
            try {
                // Add messages to timeline using the renderer's message system
                renderer.addUserMessage("/init $args")

                // Start processing indicator
                renderer.renderLLMResponseStart()
                renderer.renderLLMResponseChunk("🚀 Starting domain dictionary generation...")
                renderer.renderLLMResponseEnd()

                // Load configuration
                val configWrapper = ConfigManager.load()
                val modelConfig = configWrapper.getActiveModelConfig()

                if (modelConfig == null) {
                    renderer.renderError("❌ No LLM configuration found. Please configure your model first.")
                    return@launch
                }

                renderer.renderLLMResponseStart()
                renderer.renderLLMResponseChunk("📊 Analyzing project code...")
                renderer.renderLLMResponseEnd()

                // Create domain dictionary generator
                val fileSystem = DefaultProjectFileSystem(projectPath)
                val generator = DomainDictGenerator(fileSystem = fileSystem, modelConfig = modelConfig)

                // Check if domain dictionary already exists
                if (!force && fileSystem.exists("prompts/domain.csv")) {
                    renderer.renderError("⚠️ Domain dictionary already exists at prompts/domain.csv\nUse /init --force to regenerate")
                    return@launch
                }

                renderer.renderLLMResponseStart()
                renderer.renderLLMResponseChunk("🤖 Generating domain dictionary with AI...")
                renderer.renderLLMResponseEnd()

                // Generate domain dictionary
                val result = generator.generateAndSave()

                when (result) {
                    is cc.unitmesh.indexer.GenerationResult.Success -> {
                        renderer.renderLLMResponseStart()
                        renderer.renderLLMResponseChunk("💾 Saving domain dictionary to prompts/domain.csv...")
                        renderer.renderLLMResponseEnd()
                        renderer.renderFinalResult(true, "✅ Domain dictionary generated successfully! File saved to prompts/domain.csv", 1)
                    }
                    is cc.unitmesh.indexer.GenerationResult.Error -> {
                        renderer.renderError("❌ Domain dictionary generation failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                renderer.renderError("❌ Domain dictionary generation failed: ${e.message}")
            }
        }
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

        val allBuiltinTools = ToolType.ALL_TOOLS.filter { it.category != ToolCategory.SubAgent }
        val builtinToolsEnabled =
            if (toolConfig != null) {
                allBuiltinTools.count { toolType ->
                    toolType.name in toolConfig.enabledBuiltinTools
                }
            } else {
                allBuiltinTools.size
            }

        val subAgentTools = ToolType.byCategory(ToolCategory.SubAgent)
        val subAgentsEnabled =
            if (toolConfig != null) {
                subAgentTools.count { toolType ->
                    toolType.name in toolConfig.enabledBuiltinTools
                }
            } else {
                subAgentTools.size
            }

        val mcpServersTotal = toolConfig?.mcpServers?.filter { !it.value.disabled }?.size ?: 0
        val mcpServersLoaded = mcpPreloadingStatus.preloadedServers.size

        val mcpToolsEnabled =
            if (McpToolConfigManager.isPreloading()) {
                0
            } else {
                val enabledMcpToolsCount = toolConfig?.enabledMcpTools?.size ?: 0
                if (enabledMcpToolsCount > 0) {
                    enabledMcpToolsCount
                } else {
                    mcpPreloadingStatus.preloadedServers.sumOf { _ -> 0 }
                }
            }

        val mcpToolsTotal =
            if (McpToolConfigManager.isPreloading()) {
                0
            } else {
                McpToolConfigManager.getTotalDiscoveredTools()
            }

        return ToolLoadingStatus(
            builtinToolsEnabled = builtinToolsEnabled,
            builtinToolsTotal = allBuiltinTools.size,
            subAgentsEnabled = subAgentsEnabled,
            subAgentsTotal = subAgentTools.size,
            mcpServersLoaded = mcpServersLoaded,
            mcpServersTotal = mcpServersTotal,
            mcpToolsEnabled = mcpToolsEnabled,
            mcpToolsTotal = mcpToolsTotal,
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
    val mcpToolsTotal: Int = 0,
    val isLoading: Boolean = false
)
