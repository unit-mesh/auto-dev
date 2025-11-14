package cc.unitmesh.devins.ui.compose.agent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.CodeReviewAgent
import cc.unitmesh.agent.ReviewTask
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
 * Compose ViewModel for Multi-Agent support (Coding Agent + Code Review Agent)
 *
 * Uses the new BaseRenderer architecture with ComposeRenderer
 * for consistent rendering across CLI, TUI, and Compose UI
 *
 * 支持会话管理：Agent 模式的对话也会保存到 ChatHistoryManager
 * 支持多 Agent 切换：CodingAgent 和 CodeReviewAgent
 */
class CodingAgentViewModel(
    private val llmService: KoogLLMService?,
    private val projectPath: String,
    private val maxIterations: Int = 100,
    private val chatHistoryManager: ChatHistoryManager? = null  // 新增：会话管理
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val renderer = ComposeRenderer()

    // Current agent type
    var currentAgentType by mutableStateOf(AgentType.CODING)
        private set

    // Lazy initialization of agents to handle async tool config loading
    private var _codingAgent: CodingAgent? = null
    private var _codeReviewAgent: CodeReviewAgent? = null
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

    private var cachedToolConfig: cc.unitmesh.agent.config.ToolConfigFile? = null

    init {
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
            cachedToolConfig = toolConfig

            if (toolConfig.mcpServers.isEmpty()) {
                mcpPreloadingMessage = "No MCP servers configured"
                return
            }

            mcpPreloadingMessage = "Initializing ${toolConfig.mcpServers.size} MCP servers..."

            McpToolConfigManager.init(toolConfig)

            while (McpToolConfigManager.isPreloading()) {
                mcpPreloadingStatus = McpToolConfigManager.getPreloadingStatus()
                mcpPreloadingMessage = "Loading MCP servers... (${mcpPreloadingStatus.preloadedServers.size} completed)"
                delay(500) // Update every 500ms
            }

            delay(1000)

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

    private suspend fun initializeCodeReviewAgent(): CodeReviewAgent {
        if (llmService == null) {
            throw IllegalStateException("LLM service is not configured")
        }

        if (_codeReviewAgent == null) {
            val toolConfig = ConfigManager.loadToolConfig()
            val mcpToolConfigService = McpToolConfigService(toolConfig)

            // Reuse the same pattern: create the agent directly
            _codeReviewAgent = CodeReviewAgent(
                projectPath = projectPath,
                llmService = llmService,
                maxIterations = maxIterations,
                renderer = renderer,
                mcpToolConfigService = mcpToolConfigService
            )
        }

        return _codeReviewAgent!!
    }

    fun switchAgent(agentType: AgentType) {
        if (currentAgentType != agentType) {
            currentAgentType = agentType
            println("🔄 [ViewModel] Switched to agent type: ${agentType.name}")
            // Note: Agents will be lazily initialized when needed
        }
    }

    /**
     * Check if LLM service is configured
     */
    fun isConfigured(): Boolean = llmService != null

    /**
     * Execute a code review task
     */
    fun executeReviewTask(reviewTask: ReviewTask, onConfigRequired: (() -> Unit)? = null) {
        println("📝 [ViewModel] Executing code review task: ${reviewTask.reviewType}")

        if (isExecuting) {
            println("Agent is already executing")
            return
        }

        // Check if LLM service is configured
        if (!isConfigured()) {
            renderer.renderError("⚠️ LLM model is not configured. Please configure your model to continue.")
            onConfigRequired?.invoke()
            return
        }

        isExecuting = true
        renderer.clearError()

        // Add user message describing the review
        val reviewMessage = buildString {
            append("Starting code review: ")
            append(reviewTask.reviewType.name.lowercase().replace("_", " "))
            if (reviewTask.filePaths.isNotEmpty()) {
                append(" for ${reviewTask.filePaths.size} file(s)")
            }
        }
        renderer.addUserMessage(reviewMessage)

        currentExecutionJob = scope.launch {
            try {
                println("🔧 [ViewModel] Initializing CodeReviewAgent...")
                val codeReviewAgent = initializeCodeReviewAgent()
                chatHistoryManager?.addUserMessage(reviewMessage)

                println("▶️ [ViewModel] Executing CodeReviewAgent task...")
                val result = codeReviewAgent.executeTask(reviewTask)

                val resultSummary = "Code review completed: ${reviewTask.reviewType}"
                chatHistoryManager?.addAssistantMessage(resultSummary)

                // Result is already handled by the renderer
                isExecuting = false
                currentExecutionJob = null
            } catch (e: CancellationException) {
                // Task was cancelled
                renderer.forceStop()
                renderer.renderError("Code review cancelled by user")
                isExecuting = false
                currentExecutionJob = null
            } catch (e: Exception) {
                renderer.renderError(e.message ?: "Unknown error during code review")
                isExecuting = false
                currentExecutionJob = null
            }
        }
    }

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

        isExecuting = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentExecutionJob =
            scope.launch {
                try {
                    val codingAgent = initializeCodingAgent()
                    chatHistoryManager?.addUserMessage(task)
                    val agentTask =
                        AgentTask(
                            requirement = task,
                            projectPath = projectPath
                        )

                    val result = codingAgent.executeTask(agentTask)

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

    fun cancelTask() {
        if (isExecuting && currentExecutionJob != null) {
            currentExecutionJob?.cancel("Task cancelled by user")
            currentExecutionJob = null
            isExecuting = false
        }
    }


    fun newSession() {
        renderer.clearMessages()
        chatHistoryManager?.createSession()
    }

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

    fun clearError() {
        renderer.clearError()
    }

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
