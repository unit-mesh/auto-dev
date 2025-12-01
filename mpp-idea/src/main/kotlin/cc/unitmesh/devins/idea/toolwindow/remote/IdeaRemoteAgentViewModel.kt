package cc.unitmesh.devins.idea.toolwindow.remote

import cc.unitmesh.agent.RemoteAgentEvent
import cc.unitmesh.devins.idea.renderer.JewelRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for Remote Agent in IntelliJ IDEA plugin.
 *
 * Connects to mpp-server and streams agent execution events,
 * forwarding them to JewelRenderer for UI rendering.
 *
 * This is adapted from mpp-ui's RemoteCodingAgentViewModel.
 */
class IdeaRemoteAgentViewModel(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    serverUrl: String = "http://localhost:8080",
    private val useServerConfig: Boolean = false
) : Disposable {
    
    private var _serverUrl = serverUrl
    val serverUrl: String get() = _serverUrl
    
    private var client = IdeaRemoteAgentClient(_serverUrl)

    val renderer = JewelRenderer()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _availableProjects = MutableStateFlow<List<ProjectInfo>>(emptyList())
    val availableProjects: StateFlow<List<ProjectInfo>> = _availableProjects.asStateFlow()

    private var currentExecutionJob: Job? = null

    /**
     * Update server URL and recreate client
     */
    fun updateServerUrl(newUrl: String) {
        if (newUrl != _serverUrl) {
            _serverUrl = newUrl
            client.close()
            client = IdeaRemoteAgentClient(_serverUrl)
            _isConnected.value = false
            _connectionError.value = null
            _availableProjects.value = emptyList()
        }
    }

    /**
     * Check connection to server
     */
    fun checkConnection() {
        coroutineScope.launch {
            try {
                val health = client.healthCheck()
                _isConnected.value = health.status == "ok"
                _connectionError.value = null

                if (_isConnected.value) {
                    val projectList = client.getProjects()
                    _availableProjects.value = projectList.projects
                }
            } catch (e: Exception) {
                _isConnected.value = false
                _connectionError.value = e.message ?: "Failed to connect to server"
            }
        }
    }

    /**
     * Execute a task on the remote server
     */
    fun executeTask(projectId: String, task: String, gitUrl: String = "") {
        if (_isExecuting.value) {
            println("Agent is already executing")
            return
        }

        if (!_isConnected.value) {
            renderer.renderError("Not connected to server. Please check server URL.")
            return
        }

        _isExecuting.value = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentExecutionJob = coroutineScope.launch {
            try {
                val llmConfig = if (!useServerConfig) {
                    val config = ConfigManager.load()
                    val activeConfig = config.getActiveModelConfig()

                    if (activeConfig == null) {
                        renderer.renderError("No active LLM configuration found. Please configure your model first.")
                        _isExecuting.value = false
                        return@launch
                    }

                    LLMConfig(
                        provider = activeConfig.provider.name,
                        modelName = activeConfig.modelName,
                        apiKey = activeConfig.apiKey ?: "",
                        baseUrl = activeConfig.baseUrl
                    )
                } else {
                    null
                }

                val request = buildRequest(projectId, task, gitUrl, llmConfig)

                client.executeStream(request).collect { event ->
                    handleRemoteEvent(event)

                    if (event is RemoteAgentEvent.Complete) {
                        _isExecuting.value = false
                        currentExecutionJob = null
                    }
                }

            } catch (e: CancellationException) {
                renderer.forceStop()
                renderer.renderError("Task cancelled by user")
                _isExecuting.value = false
                currentExecutionJob = null
            } catch (e: Exception) {
                renderer.renderError(e.message ?: "Unknown error")
                _isExecuting.value = false
                currentExecutionJob = null
            }
        }
    }

    private fun buildRequest(
        projectId: String,
        task: String,
        gitUrl: String,
        llmConfig: LLMConfig?
    ): RemoteAgentRequest {
        return if (gitUrl.isNotBlank()) {
            RemoteAgentRequest(
                projectId = extractProjectIdFromUrl(gitUrl) ?: "temp-project",
                task = task,
                llmConfig = llmConfig,
                gitUrl = gitUrl
            )
        } else {
            val isGitUrl = projectId.startsWith("http://") ||
                    projectId.startsWith("https://") ||
                    projectId.startsWith("git@")

            if (isGitUrl) {
                RemoteAgentRequest(
                    projectId = extractProjectIdFromUrl(projectId) ?: "temp-project",
                    task = task,
                    llmConfig = llmConfig,
                    gitUrl = projectId
                )
            } else {
                RemoteAgentRequest(
                    projectId = projectId,
                    task = task,
                    llmConfig = llmConfig
                )
            }
        }
    }

    /**
     * Extract project ID from a Git URL, handling trailing slashes and empty segments.
     */
    private fun extractProjectIdFromUrl(url: String): String? {
        return url.trimEnd('/')
            .split('/')
            .lastOrNull { it.isNotBlank() }
            ?.removeSuffix(".git")
            ?.ifBlank { null }
    }

    /**
     * Handle remote events and forward to JewelRenderer
     */
    private fun handleRemoteEvent(event: RemoteAgentEvent) {
        when (event) {
            is RemoteAgentEvent.CloneProgress -> {
                if (event.progress != null) {
                    renderer.renderLLMResponseStart()
                    renderer.renderLLMResponseChunk("📦 Cloning repository: ${event.stage} (${event.progress}%)")
                    renderer.renderLLMResponseEnd()
                }
            }

            is RemoteAgentEvent.CloneLog -> {
                if (!event.isError && (event.message.contains("✓") || event.message.contains("ready"))) {
                    renderer.renderLLMResponseStart()
                    renderer.renderLLMResponseChunk(event.message)
                    renderer.renderLLMResponseEnd()
                } else if (event.isError) {
                    renderer.renderError(event.message)
                }
            }

            is RemoteAgentEvent.Iteration -> {
                renderer.renderIterationHeader(event.current, event.max)
            }

            is RemoteAgentEvent.LLMChunk -> {
                if (!renderer.isProcessing.value) {
                    renderer.renderLLMResponseStart()
                }
                renderer.renderLLMResponseChunk(event.chunk)
            }

            is RemoteAgentEvent.ToolCall -> {
                if (renderer.isProcessing.value) {
                    renderer.renderLLMResponseEnd()
                }
                renderer.renderToolCall(event.toolName, event.params)
            }

            is RemoteAgentEvent.ToolResult -> {
                renderer.renderToolResult(
                    toolName = event.toolName,
                    success = event.success,
                    output = event.output,
                    fullOutput = event.output,
                    metadata = emptyMap()
                )
            }

            is RemoteAgentEvent.Error -> {
                renderer.renderError(event.message)
            }

            is RemoteAgentEvent.Complete -> {
                if (renderer.isProcessing.value) {
                    renderer.renderLLMResponseEnd()
                }
                renderer.renderFinalResult(event.success, event.message, event.iterations)
            }
        }
    }

    /**
     * Cancel current task
     */
    fun cancelTask() {
        if (_isExecuting.value && currentExecutionJob != null) {
            currentExecutionJob?.cancel("Task cancelled by user")
            currentExecutionJob = null
            _isExecuting.value = false
        }
    }

    /**
     * Clear chat history
     */
    fun clearHistory() {
        renderer.clearTimeline()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        renderer.clearError()
        _connectionError.value = null
    }

    override fun dispose() {
        currentExecutionJob?.cancel()
        client.close()
    }
}

