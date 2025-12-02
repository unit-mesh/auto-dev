package cc.unitmesh.devins.idea.services

import cc.unitmesh.agent.config.ToolConfigFile
import cc.unitmesh.devins.ui.config.ConfigManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * Project-level service for managing tool configuration state.
 *
 * This service provides a centralized way to:
 * 1. Load and cache tool configuration
 * 2. Notify listeners when configuration changes
 * 3. Track enabled/disabled MCP tools count
 *
 * Components like IdeaToolLoadingStatusBar and IdeaAgentViewModel can observe
 * the toolConfigState to react to configuration changes.
 */
@Service(Service.Level.PROJECT)
class IdeaToolConfigService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(IdeaToolConfigService::class.java)

    // Tool configuration state
    private val _toolConfigState = MutableStateFlow(ToolConfigState())
    val toolConfigState: StateFlow<ToolConfigState> = _toolConfigState.asStateFlow()

    // Version counter to force recomposition when config changes
    private val _configVersion = MutableStateFlow(0L)
    val configVersion: StateFlow<Long> = _configVersion.asStateFlow()

    init {
        // Load initial configuration
        reloadConfig()
    }

    /**
     * Reload configuration from disk and update state.
     * Uses runBlocking since this is called from non-suspend context.
     */
    fun reloadConfig() {
        try {
            val toolConfig = runBlocking { ConfigManager.loadToolConfig() }
            updateState(toolConfig)
            logger.debug("Tool configuration reloaded: ${toolConfig.enabledMcpTools.size} enabled tools")
        } catch (e: Exception) {
            logger.warn("Failed to reload tool configuration: ${e.message}")
        }
    }

    /**
     * Update the tool configuration state.
     * Call this after saving configuration changes.
     */
    fun updateState(toolConfig: ToolConfigFile) {
        val enabledMcpToolsCount = toolConfig.enabledMcpTools.size
        val mcpServersCount = toolConfig.mcpServers.filter { !it.value.disabled }.size

        _toolConfigState.value = ToolConfigState(
            toolConfig = toolConfig,
            enabledMcpToolsCount = enabledMcpToolsCount,
            mcpServersCount = mcpServersCount,
            lastUpdated = System.currentTimeMillis()
        )

        // Increment version to trigger recomposition
        _configVersion.value++

        logger.debug("Tool config state updated: $enabledMcpToolsCount enabled tools, $mcpServersCount servers")
    }

    /**
     * Save tool configuration and update state.
     * Uses runBlocking since this is called from non-suspend context.
     */
    fun saveAndUpdateConfig(toolConfig: ToolConfigFile) {
        try {
            runBlocking { ConfigManager.saveToolConfig(toolConfig) }
            updateState(toolConfig)
            logger.debug("Tool configuration saved and state updated")
        } catch (e: Exception) {
            logger.error("Failed to save tool configuration: ${e.message}")
        }
    }

    /**
     * Get the current tool configuration.
     */
    fun getToolConfig(): ToolConfigFile {
        return _toolConfigState.value.toolConfig
    }

    override fun dispose() {
        // Cleanup if needed
    }

    companion object {
        fun getInstance(project: Project): IdeaToolConfigService = project.service()
    }
}

/**
 * Data class representing the current tool configuration state.
 */
data class ToolConfigState(
    val toolConfig: ToolConfigFile = ToolConfigFile.default(),
    val enabledMcpToolsCount: Int = 0,
    val mcpServersCount: Int = 0,
    val lastUpdated: Long = 0L
)

