package cc.unitmesh.devins.ui.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cc.unitmesh.agent.RemoteAgentEvent
import cc.unitmesh.devins.ui.compose.agent.ComposeRenderer
import cc.unitmesh.devins.ui.config.ConfigManager
import kotlinx.coroutines.*

/**
 * ViewModel for Remote Coding Agent
 *
 * Connects to mpp-server and streams agent execution events,
 * forwarding them to ComposeRenderer for UI rendering.
 *
 * This is the Compose equivalent of runServerAgent() in index.tsx
 */
class RemoteCodingAgentViewModel(
    private val serverUrl: String,
    private val useServerConfig: Boolean = false
) {
    // ⚠️ 使用 Dispatchers.Main 确保 UI 状态更新在主线程，实现流式渲染
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val client = RemoteAgentClient(serverUrl)

    val renderer = ComposeRenderer()

    var isExecuting by mutableStateOf(false)
        private set

    var isConnected by mutableStateOf(false)
        private set

    var connectionError by mutableStateOf<String?>(null)
        private set

    var availableProjects by mutableStateOf<List<ProjectInfo>>(emptyList())
        private set

    private var currentExecutionJob: Job? = null

    // TreeView state (same as local ViewModel)
    var isTreeViewVisible by mutableStateOf(false)

    fun toggleTreeView() {
        isTreeViewVisible = !isTreeViewVisible
    }

    fun closeTreeView() {
        isTreeViewVisible = false
    }

    suspend fun checkConnection(): Boolean {
        return try {
            val health = client.healthCheck()
            isConnected = health.status == "ok"
            connectionError = null

            // Load available projects if connected
            if (isConnected) {
                val projectList = client.getProjects()
                availableProjects = projectList.projects
            }

            isConnected
        } catch (e: Exception) {
            isConnected = false
            connectionError = e.message ?: "Failed to connect to server"
            false
        }
    }

    /**
     * Execute a task on the remote server
     *
     * @param projectId The project ID on the server
     * @param task The task description
     * @param gitUrl Optional Git URL to clone (overrides projectId as source)
     */
    fun executeTask(projectId: String, task: String, gitUrl: String = "") {
        if (isExecuting) {
            println("Agent is already executing")
            return
        }

        if (!isConnected) {
            renderer.renderError("Not connected to server. Please check server URL.")
            return
        }

        isExecuting = true
        renderer.clearError()
        renderer.addUserMessage(task)

        currentExecutionJob = scope.launch {
            try {
                // Determine LLM config source
                val llmConfig = if (!useServerConfig) {
                    // Load from local config
                    val config = ConfigManager.load()
                    val activeConfig = config.getActiveModelConfig()

                    if (activeConfig == null) {
                        renderer.renderError("No active LLM configuration found. Please configure your model first.")
                        isExecuting = false
                        return@launch
                    }

                    LLMConfig(
                        provider = activeConfig.provider.name,
                        modelName = activeConfig.modelName,
                        apiKey = activeConfig.apiKey ?: "",
                        baseUrl = activeConfig.baseUrl
                    )
                } else {
                    // Use server's config
                    null
                }

                // Determine request parameters
                val request = if (gitUrl.isNotBlank()) {
                    // Explicit gitUrl provided
                    RemoteAgentRequest(
                        projectId = gitUrl.split('/').lastOrNull()?.removeSuffix(".git") ?: "temp-project",
                        task = task,
                        llmConfig = llmConfig,
                        gitUrl = gitUrl
                    )
                } else {
                    // Check if projectId is actually a git URL
                    val isGitUrl = projectId.startsWith("http://") ||
                                  projectId.startsWith("https://") ||
                                  projectId.startsWith("git@")

                    if (isGitUrl) {
                        RemoteAgentRequest(
                            projectId = projectId.split('/').lastOrNull()?.removeSuffix(".git") ?: "temp-project",
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

                // Stream events from server and forward to renderer
                // ⚠️ 关键：collect 在 Main 线程执行（由 scope 决定），确保 UI 状态实时更新
                client.executeStream(request).collect { event ->
                    handleRemoteEvent(event)

                    // Stop on complete
                    if (event is RemoteAgentEvent.Complete) {
                        isExecuting = false
                        currentExecutionJob = null
                    }
                }

            } catch (e: CancellationException) {
                renderer.forceStop()
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
     * Handle remote events and forward to ComposeRenderer
     *
     * This is the key bridge: converts RemoteAgentEvent to renderer calls
     */
    private fun handleRemoteEvent(event: RemoteAgentEvent) {
        when (event) {
            is RemoteAgentEvent.CloneProgress -> {
                // Clone progress can be shown as a message
                if (event.progress != null) {
                    renderer.renderLLMResponseStart()
                    renderer.renderLLMResponseChunk("📦 Cloning repository: ${event.stage} (${event.progress}%)")
                    renderer.renderLLMResponseEnd()
                }
            }

            is RemoteAgentEvent.CloneLog -> {
                // Show important clone logs
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
                // Start LLM response if not already started
                if (!renderer.isProcessing) {
                    renderer.renderLLMResponseStart()
                }
                renderer.renderLLMResponseChunk(event.chunk)
            }

            is RemoteAgentEvent.ToolCall -> {
                // End any ongoing LLM response before tool call
                if (renderer.isProcessing) {
                    renderer.renderLLMResponseEnd()
                }
                renderer.renderToolCall(event.toolName, event.params)
            }

            is RemoteAgentEvent.ToolResult -> {
                // For DocQL, try to extract detailed results from output if available
                // The output should be compact summary, and detailed results should be in metadata
                // But since RemoteAgentEvent doesn't include metadata yet, we use output for both
                // TODO: Update RemoteAgentEvent.ToolResult to include metadata
                val output = event.output
                val fullOutput = event.output // Will be updated when metadata is available
                
                renderer.renderToolResult(
                    toolName = event.toolName,
                    success = event.success,
                    output = output,
                    fullOutput = fullOutput,
                    metadata = emptyMap()
                )
            }

            is RemoteAgentEvent.Error -> {
                renderer.renderError(event.message)
            }

            is RemoteAgentEvent.Complete -> {
                // End any ongoing LLM response
                if (renderer.isProcessing) {
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
     * Create new session (for Remote, just clear messages)
     */
    fun newSession() {
        renderer.clearMessages()
    }

    /**
     * Clear error state
     */
    fun clearError() {
        renderer.clearError()
        connectionError = null
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        client.close()
        scope.cancel()
    }
}

