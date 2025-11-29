package cc.unitmesh.devins.idea.toolwindow

import cc.unitmesh.agent.AgentTask
import cc.unitmesh.agent.AgentType
import cc.unitmesh.agent.CodingAgent
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.config.ToolConfigFile
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
 */
class IdeaAgentViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) : Disposable {

    // Renderer for agent output (uses StateFlow instead of Compose mutableStateOf)
    val renderer = JewelRenderer()

    // Current agent type tab (using mpp-core's AgentType)
    private val _currentAgentType = MutableStateFlow(AgentType.CODING)
    val currentAgentType: StateFlow<AgentType> = _currentAgentType.asStateFlow()

    // Is processing a request
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // LLM Configuration from mpp-ui's ConfigManager
    private val _configWrapper = MutableStateFlow<AutoDevConfigWrapper?>(null)
    val configWrapper: StateFlow<AutoDevConfigWrapper?> = _configWrapper.asStateFlow()

    // Current model config (derived from configWrapper)
    private val _currentModelConfig = MutableStateFlow<ModelConfig?>(null)
    val currentModelConfig: StateFlow<ModelConfig?> = _currentModelConfig.asStateFlow()

    // LLM Service (created from config)
    private var llmService: KoogLLMService? = null

    // CodingAgent instance
    private var codingAgent: CodingAgent? = null
    private var agentInitialized = false

    // Show config dialog
    private val _showConfigDialog = MutableStateFlow(false)
    val showConfigDialog: StateFlow<Boolean> = _showConfigDialog.asStateFlow()

    // Current execution job (for cancellation)
    private var currentJob: Job? = null

    init {
        // Load configuration on initialization
        loadConfiguration()
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
                if (modelConfig != null && modelConfig.isValid()) {
                    llmService = KoogLLMService.create(modelConfig)
                }

                // Set agent type from config
                _currentAgentType.value = wrapper.getAgentType()
            } catch (e: Exception) {
                // Config file doesn't exist or is invalid, use defaults
                _configWrapper.value = null
                _currentModelConfig.value = null
                llmService = null
            }
        }
    }

    /**
     * Reload configuration from file
     */
    fun reloadConfiguration() {
        agentInitialized = false
        codingAgent = null
        loadConfiguration()
    }

    /**
     * Change the current agent type tab.
     */
    fun onAgentTypeChange(agentType: AgentType) {
        _currentAgentType.value = agentType
    }

    /**
     * Initialize the CodingAgent with tool configuration
     */
    private suspend fun initializeCodingAgent(): CodingAgent {
        val service = llmService
            ?: throw IllegalStateException("LLM service is not configured. Please configure in ~/.autodev/config.yaml")

        if (codingAgent == null || !agentInitialized) {
            val toolConfig = try {
                ConfigManager.loadToolConfig()
            } catch (e: Exception) {
                ToolConfigFile.default()
            }

            val mcpToolConfigService = McpToolConfigService(toolConfig)
            val projectPath = project.basePath ?: System.getProperty("user.home")

            codingAgent = CodingAgent(
                projectPath = projectPath,
                llmService = service,
                maxIterations = 100,
                renderer = renderer,
                mcpToolConfigService = mcpToolConfigService,
                enableLLMStreaming = true
            )
            agentInitialized = true
        }
        return codingAgent!!
    }

    /**
     * Send a message to the Agent.
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || _isProcessing.value) return

        // Add user message to renderer timeline
        renderer.addUserMessage(content)

        // Start processing
        _isProcessing.value = true

        currentJob = coroutineScope.launch {
            try {
                val agent = initializeCodingAgent()
                val projectPath = project.basePath ?: System.getProperty("user.home")

                val task = AgentTask(
                    requirement = content,
                    projectPath = projectPath
                )

                // Execute the agent task
                agent.executeTask(task)

            } catch (e: CancellationException) {
                renderer.renderError("Task cancelled by user")
            } catch (e: Exception) {
                renderer.renderError("Error: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Abort the current request.
     */
    fun abortRequest() {
        currentJob?.cancel()
        _isProcessing.value = false
    }

    /**
     * Clear chat history.
     */
    fun clearHistory() {
        renderer.clearTimeline()
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

