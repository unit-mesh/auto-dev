package cc.unitmesh.devins.idea.toolwindow

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.AgentType
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.PreloadingStatus
import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.schema.ToolCategory
import cc.unitmesh.devins.compiler.service.DevInsCompilerService
import cc.unitmesh.devins.idea.compiler.IdeaDevInsCompilerService
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.config.AutoDevConfigWrapper
import cc.unitmesh.devins.ui.config.ConfigManager
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig
import cc.unitmesh.llm.NamedModelConfig
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Agent ToolWindow.
 * Manages agent type tabs, chat messages, and LLM integration.
 *
 * Uses mpp-ui's ConfigManager for configuration management to ensure
 * cross-platform consistency with CLI and Desktop apps.
 *
 * Integrates with mpp-core's CodingAgent for actual agent execution.
 *
 * Aligned with CodingAgentViewModel from mpp-ui for feature parity.
 */
class IdeaAgentViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val maxIterations: Int = 100
) : Disposable {

    // Renderer for agent output (uses StateFlow instead of Compose mutableStateOf)
    val renderer = JewelRenderer()

    // Current agent type tab (using mpp-core's AgentType)
    // Initialize with value from config to avoid flicker
    private val _currentAgentType = MutableStateFlow(loadInitialAgentType())
    val currentAgentType: StateFlow<AgentType> = _currentAgentType.asStateFlow()

    // Is executing a task
    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    // Is processing (alias for isExecuting for backward compatibility)
    val isProcessing: StateFlow<Boolean> get() = _isExecuting

    // LLM Configuration from mpp-ui's ConfigManager
    private val _configWrapper = MutableStateFlow<AutoDevConfigWrapper?>(null)
    val configWrapper: StateFlow<AutoDevConfigWrapper?> = _configWrapper.asStateFlow()

    // Current model config (derived from configWrapper)
    private val _currentModelConfig = MutableStateFlow<ModelConfig?>(null)
    val currentModelConfig: StateFlow<ModelConfig?> = _currentModelConfig.asStateFlow()

    // LLM Service (created from config)
    private var llmService: KoogLLMService? = null

    // IDEA DevIns Compiler Service (uses PSI-based compiler with full IDE features)
    private val ideaCompilerService: DevInsCompilerService by lazy {
        IdeaDevInsCompilerService.create(project)
    }

    // CodingAgent instance
    private var codingAgent: CodingAgent? = null
    private var agentInitialized = false

    // Show config dialog
    private val _showConfigDialog = MutableStateFlow(false)
    val showConfigDialog: StateFlow<Boolean> = _showConfigDialog.asStateFlow()

    // Current execution job (for cancellation)
    private var currentJob: Job? = null

    // MCP Preloading status
    private val _mcpPreloadingStatus = MutableStateFlow(PreloadingStatus(false, emptyList(), 0))
    val mcpPreloadingStatus: StateFlow<PreloadingStatus> = _mcpPreloadingStatus.asStateFlow()

    private val _mcpPreloadingMessage = MutableStateFlow("")
    val mcpPreloadingMessage: StateFlow<String> = _mcpPreloadingMessage.asStateFlow()

    // Cached tool configuration
    private var cachedToolConfig: ToolConfigFile? = null

    init {
        // Load configuration on initialization
        loadConfiguration()
    }

    /**
     * Load initial agent type synchronously to avoid UI flicker.
     * This is called during initialization before the UI is rendered.
     */
    private fun loadInitialAgentType(): AgentType {
        return try {
            // Use runBlocking to load config synchronously during initialization
            // This is acceptable here as it only happens once during ViewModel creation
            runBlocking {
                val wrapper = ConfigManager.load()
                wrapper.getAgentType()
            }
        } catch (e: Exception) {
            // If config doesn't exist or is invalid, default to CODING
            AgentType.CODING
        }
    }

    /**
     * Load configuration from ConfigManager (~/.autodev/config.yaml)
     */
    private fun loadConfiguration() {
        coroutineScope.launch {
            try {
                val wrapper = ConfigManager.load()
                _configWrapper.value = wrapper
                val modelConfig = wrapper.getActiveModelConfig()
                _currentModelConfig.value = modelConfig

                // Create LLM service if config is valid
                // Inject IDEA compiler service for full IDE feature support
                if (modelConfig != null && modelConfig.isValid()) {
                    llmService = KoogLLMService(
                        config = modelConfig,
                        compilerService = ideaCompilerService
                    )
                    // Start MCP preloading after LLM service is created
                    startMcpPreloading()
                }

                // Agent type is already loaded in initialization, no need to update again
                // This prevents the flicker issue where the tab changes after UI is rendered
            } catch (e: Exception) {
                // Config file doesn't exist or is invalid, use defaults
                _configWrapper.value = null
                _currentModelConfig.value = null
                llmService = null
            }
        }
    }

    /**
     * Start MCP servers preloading in background.
     * Aligned with CodingAgentViewModel's startMcpPreloading().
     */
    private suspend fun startMcpPreloading() {
        try {
            _mcpPreloadingMessage.value = "Loading MCP servers configuration..."
            val toolConfig = ConfigManager.loadToolConfig()
            cachedToolConfig = toolConfig

            if (toolConfig.mcpServers.isEmpty()) {
                _mcpPreloadingMessage.value = "No MCP servers configured"
                return
            }

            _mcpPreloadingMessage.value = "Initializing ${toolConfig.mcpServers.size} MCP servers..."

            // Initialize MCP servers (this will start background preloading)
            McpToolConfigManager.init(toolConfig)

            // Monitor preloading status with timeout to prevent infinite loop
            val timeoutMs = 60_000L // 60 seconds max
            val startTime = System.currentTimeMillis()
            while (McpToolConfigManager.isPreloading() &&
                (System.currentTimeMillis() - startTime) < timeoutMs
            ) {
                _mcpPreloadingStatus.value = McpToolConfigManager.getPreloadingStatus()
                _mcpPreloadingMessage.value = "Loading MCP servers... (${_mcpPreloadingStatus.value.preloadedServers.size} completed)"
                delay(500)
            }

            // Final status update
            _mcpPreloadingStatus.value = McpToolConfigManager.getPreloadingStatus()

            val preloadedCount = _mcpPreloadingStatus.value.preloadedServers.size
            val totalCount = toolConfig.mcpServers.filter { !it.value.disabled }.size

            _mcpPreloadingMessage.value = if (preloadedCount > 0) {
                "MCP servers loaded successfully ($preloadedCount/$totalCount servers)"
            } else {
                "MCP servers initialization completed (no tools loaded)"
            }
        } catch (e: Exception) {
            _mcpPreloadingMessage.value = "Failed to load MCP servers: ${e.message}"
        }
    }

    /**
     * Reload configuration from file
     */
    fun reloadConfiguration() {
        agentInitialized = false
        codingAgent = null
        cachedToolConfig = null
        loadConfiguration()
    }

    /**
     * Change the current agent type tab and persist to config.
     */
    fun onAgentTypeChange(agentType: AgentType) {
        _currentAgentType.value = agentType

        // Save to config file for persistence
        coroutineScope.launch {
            try {
                val typeString = when (agentType) {
                    AgentType.REMOTE -> "Remote"
                    AgentType.LOCAL_CHAT -> "Local"
                    AgentType.CODING -> "Coding"
                    AgentType.CODE_REVIEW -> "CodeReview"
                    AgentType.KNOWLEDGE -> "Documents"
                }
                cc.unitmesh.devins.ui.config.saveAgentTypePreference(typeString)
            } catch (e: Exception) {
                // Silently fail - not critical if we can't save preference
                println("⚠️ Failed to save agent type preference: ${e.message}")
            }
        }
    }

    /**
     * Initialize the CodingAgent with tool configuration.
     * Aligned with CodingAgentViewModel's initializeCodingAgent().
     */
    private suspend fun initializeCodingAgent(): CodingAgent {
        val service = llmService
            ?: throw IllegalStateException("LLM service is not configured. Please configure in ~/.autodev/config.yaml")

        if (codingAgent == null || !agentInitialized) {
            val toolConfig = cachedToolConfig ?: try {
                ConfigManager.loadToolConfig().also { cachedToolConfig = it }
            } catch (e: Exception) {
                ToolConfigFile.default()
            }

            val mcpToolConfigService = McpToolConfigService(toolConfig)
            val projectPath = project.basePath ?: System.getProperty("user.home")

            codingAgent = CodingAgent(
                projectPath = projectPath,
                llmService = service,
                maxIterations = maxIterations,
                renderer = renderer,
                mcpToolConfigService = mcpToolConfigService,
                enableLLMStreaming = true
            )
            agentInitialized = true
        }
        return codingAgent!!
    }

    /**
     * Check if LLM service is configured.
     */
    fun isConfigured(): Boolean = llmService != null

    /**
     * Execute a task with the agent.
     * Aligned with CodingAgentViewModel's executeTask().
     *
     * @param task The task description
     * @param onConfigRequired Callback when configuration is needed
     */
    fun executeTask(task: String, onConfigRequired: (() -> Unit)? = null) {
        if (_isExecuting.value) return

        // Handle builtin commands first (before config check) so they work without LLM configuration
        if (task.trim().startsWith("/")) {
            handleBuiltinCommand(task.trim(), onConfigRequired)
            return
        }

        if (!isConfigured()) {
            renderer.addUserMessage(task)
            renderer.renderError("WARNING: LLM model is not configured. Please configure your model to continue.")
            onConfigRequired?.invoke()
            return
        }

        _isExecuting.value = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentJob = coroutineScope.launch {
            try {
                val agent = initializeCodingAgent()
                val projectPath = project.basePath ?: System.getProperty("user.home")

                val agentTask = AgentTask(
                    requirement = task,
                    projectPath = projectPath
                )

                agent.executeTask(agentTask)
            } catch (e: CancellationException) {
                renderer.forceStop()
                renderer.renderError("Task cancelled by user")
            } catch (e: Exception) {
                renderer.renderError(e.message ?: "Unknown error")
            } finally {
                _isExecuting.value = false
                currentJob = null
            }
        }
    }

    /**
     * Handle builtin commands (/init, /clear, /help, etc.).
     * Aligned with CodingAgentViewModel's handleBuiltinCommand().
     */
    private fun handleBuiltinCommand(command: String, onConfigRequired: (() -> Unit)? = null) {
        val parts = command.substring(1).trim().split("\\s+".toRegex())
        val commandName = parts[0].lowercase()
        val args = parts.drop(1).joinToString(" ")

        renderer.addUserMessage(command)

        when (commandName) {
            "clear" -> {
                renderer.clearTimeline()
                renderer.renderFinalResult(true, "SUCCESS: Chat history cleared", 0)
            }

            "help" -> {
                val helpText = buildString {
                    appendLine("HELP: Available Commands:")
                    appendLine("  /clear - Clear chat history")
                    appendLine("  /help - Show this help message")
                    appendLine("  /status - Show MCP servers and tools status")
                    appendLine("")
                    appendLine("TIP: You can also use @ for agents and other DevIns commands")
                }
                renderer.renderFinalResult(true, helpText, 0)
            }

            "status" -> {
                val status = getToolLoadingStatus()
                val statusText = buildString {
                    appendLine("STATUS: Tool Loading Status")
                    appendLine("  SubAgents: ${status.subAgentsEnabled}/${status.subAgentsTotal}")
                    appendLine("  MCP Servers: ${status.mcpServersLoaded}/${status.mcpServersTotal}")
                    appendLine("  MCP Tools: ${status.mcpToolsEnabled}/${status.mcpToolsTotal}")
                    appendLine("  Loading: ${status.isLoading}")
                }
                renderer.renderFinalResult(true, statusText, 0)
            }

            else -> {
                if (!isConfigured()) {
                    renderer.renderError("WARNING: LLM model is not configured. Please configure your model to continue.")
                    onConfigRequired?.invoke()
                    return
                }

                // Treat as a regular task
                _isExecuting.value = true
                currentJob = coroutineScope.launch {
                    try {
                        val agent = initializeCodingAgent()
                        val projectPath = project.basePath ?: System.getProperty("user.home")
                        val agentTask = AgentTask(
                            requirement = command,
                            projectPath = projectPath
                        )
                        agent.executeTask(agentTask)
                    } catch (e: Exception) {
                        renderer.renderError(e.message ?: "Unknown error")
                    } finally {
                        _isExecuting.value = false
                        currentJob = null
                    }
                }
            }
        }
    }

    /**
     * Send a message to the Agent (alias for executeTask for backward compatibility).
     */
    fun sendMessage(content: String) {
        executeTask(content) { setShowConfigDialog(true) }
    }

    /**
     * Cancel the current task.
     */
    fun cancelTask() {
        if (_isExecuting.value && currentJob != null) {
            currentJob?.cancel("Task cancelled by user")
            currentJob = null
            renderer.forceStop()
            _isExecuting.value = false
        }
    }

    /**
     * Abort the current request (alias for cancelTask for backward compatibility).
     */
    fun abortRequest() {
        cancelTask()
    }

    /**
     * Clear chat history.
     */
    fun clearHistory() {
        renderer.clearTimeline()
    }

    /**
     * Get tool loading status.
     * Aligned with CodingAgentViewModel's getToolLoadingStatus().
     */
    fun getToolLoadingStatus(): ToolLoadingStatus {
        val toolConfig = cachedToolConfig
        val subAgentTools = ToolType.byCategory(ToolCategory.SubAgent)
        val subAgentsEnabled = subAgentTools.size
        val mcpServersTotal = toolConfig?.mcpServers?.filter { !it.value.disabled }?.size ?: 0
        val mcpServersLoaded = _mcpPreloadingStatus.value.preloadedServers.size

        val mcpToolsEnabled = if (McpToolConfigManager.isPreloading()) {
            0
        } else {
            val enabledMcpToolsCount = toolConfig?.enabledMcpTools?.size ?: 0
            if (enabledMcpToolsCount > 0) enabledMcpToolsCount else 0
        }

        val mcpToolsTotal = if (McpToolConfigManager.isPreloading()) {
            0
        } else {
            McpToolConfigManager.getTotalDiscoveredTools()
        }

        return ToolLoadingStatus(
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
     * Save a new LLM configuration using ConfigManager
     */
    fun saveModelConfig(config: NamedModelConfig, setActive: Boolean = true) {
        coroutineScope.launch {
            try {
                ConfigManager.saveConfig(config, setActive)
                // Reload configuration after saving
                reloadConfiguration()
            } catch (e: Exception) {
                // Handle save error
                renderer.renderError("Failed to save configuration: ${e.message}")
            }
        }
    }

    /**
     * Show/hide config dialog.
     */
    fun setShowConfigDialog(show: Boolean) {
        _showConfigDialog.value = show
    }

    /**
     * Set the active configuration by name
     */
    fun setActiveConfig(configName: String) {
        coroutineScope.launch {
            try {
                ConfigManager.setActive(configName)
                // Reload configuration after changing active config
                reloadConfiguration()
            } catch (e: Exception) {
                renderer.renderError("Failed to set active configuration: ${e.message}")
            }
        }
    }

    /**
     * Check if configuration is valid for LLM calls
     */
    fun isConfigValid(): Boolean {
        return _configWrapper.value?.isValid() == true
    }

    override fun dispose() {
        currentJob?.cancel()
        coroutineScope.cancel()
    }
}

/**
 * Data class to hold tool loading status information.
 * Aligned with CodingAgentViewModel's ToolLoadingStatus.
 */
data class ToolLoadingStatus(
    val subAgentsEnabled: Int = 0,
    val subAgentsTotal: Int = 0,
    val mcpServersLoaded: Int = 0,
    val mcpServersTotal: Int = 0,
    val mcpToolsEnabled: Int = 0,
    val mcpToolsTotal: Int = 0,
    val isLoading: Boolean = false
)

