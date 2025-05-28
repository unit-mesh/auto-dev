package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.util.messages.Topic
import com.intellij.openapi.application.ApplicationManager

interface SketchConfigListener {
    fun onSelectedToolsChanged(tools: Map<String, Set<Tool>>)

    companion object {
        val TOPIC = Topic.create("SketchConfigChanged", SketchConfigListener::class.java)
    }
}

@Service(Service.Level.PROJECT)
@State(
    name = "cc.unitmesh.devti.mcp.ui.McpConfigService",
    storages = [Storage("AutoDevmcpConfig.xml")]
)
class McpConfigService(private val project: Project) : PersistentStateComponent<McpConfigService.State> {
    private val selectedTools = mutableMapOf<String, MutableSet<String>>()
    private val cachedTools = mutableMapOf<Pair<String, String>, Tool>() // Cache of (serverName, toolName) -> Tool

    data class State(
        var toolSelections: Map<String, Set<String>> = emptyMap()
    )

    companion object {
        fun getInstance(project: Project): McpConfigService {
            return project.getService(McpConfigService::class.java)
        }
    }

    override fun getState(): State {
        return State(selectedTools.mapValues { it.value.toSet() })
    }

    override fun loadState(state: State) {
        selectedTools.clear()
        state.toolSelections.forEach { (server, tools) ->
            selectedTools[server] = tools.toMutableSet()
        }
    }

    fun addSelectedTool(serverName: String, toolName: String) {
        selectedTools.getOrPut(serverName) { mutableSetOf() }.add(toolName)
    }

    /**
     * Add a selected tool and cache the Tool object
     */
    fun addSelectedTool(serverName: String, tool: Tool) {
        addSelectedTool(serverName, tool.name)
        cachedTools[Pair(serverName, tool.name)] = tool
    }

    fun removeSelectedTool(serverName: String, toolName: String) {
        selectedTools[serverName]?.remove(toolName)
        if (selectedTools[serverName]?.isEmpty() == true) {
            selectedTools.remove(serverName)
        }
        cachedTools.remove(Pair(serverName, toolName))
    }

    fun isToolSelected(serverName: String, toolName: String): Boolean {
        return selectedTools[serverName]?.contains(toolName) ?: false
    }

    fun getSelectedTools(): Map<String, Set<String>> {
        return selectedTools.mapValues { it.value.toSet() }
    }

    /**
     * Gets the selected tools as actual Tool objects
     * If tools haven't been cached, it will attempt to retrieve them
     */
    suspend fun getSelectedToolObjects(): Map<String, Set<Tool>> {
        val result = mutableMapOf<String, MutableSet<Tool>>()
        // First try to get from cache
        selectedTools.forEach { (serverName, toolNames) ->
            val toolsForServer = mutableSetOf<Tool>()
            toolNames.forEach { toolName ->
                cachedTools[Pair(serverName, toolName)]?.let {
                    toolsForServer.add(it)
                }
            }
            if (toolsForServer.isNotEmpty()) {
                result[serverName] = toolsForServer
            }
        }
        return result.mapValues { it.value.toSet() }
    }

    fun clearSelectedTools() {
        selectedTools.clear()
        cachedTools.clear()
    }

    /**
     * Set selected tools from a map of server name to Tool object sets.
     */
    fun setSelectedTools(tools: Map<String, Set<Tool>>, clearCache: Boolean = false) {
        selectedTools.clear()
        tools.forEach { (serverName, toolSet) ->
            selectedTools[serverName] = toolSet.map { it.name }.toMutableSet()
            toolSet.forEach { tool -> cachedTools[Pair(serverName, tool.name)] = tool }
        }

        ApplicationManager.getApplication().messageBus
            .syncPublisher(SketchConfigListener.TOPIC)
            .onSelectedToolsChanged(tools)

        if (clearCache) {
            cachedTools.clear()
        }
    }

    /**
     * Get all available tools from MCP servers
     * @param content The content context for server configuration
     * Returns a map of server name to list of tools
     */
    suspend fun getAllAvailableTools(content: String = ""): Map<String, List<Tool>> = withContext(Dispatchers.IO) {
        val mcpServerManager = CustomMcpServerManager.instance(project)
        val allTools = mutableMapOf<String, List<Tool>>()

        val serverConfigs = mcpServerManager.getEnabledServers(content)

        if (serverConfigs.isNullOrEmpty()) {
            return@withContext emptyMap()
        }

        serverConfigs.forEach { (serverName, serverConfig) ->
            try {
                val tools = mcpServerManager.collectServerInfo(serverName, serverConfig)
                allTools[serverName] = tools
                // Cache the tools for later use
                tools.forEach { tool ->
                    cachedTools[Pair(serverName, tool.name)] = tool
                }
            } catch (e: Exception) {
                // Log error but continue with other servers
                allTools[serverName] = emptyList()
            }
        }

        allTools
    }

    /**
     * Get the total count of selected tools across all servers
     */
    fun getSelectedToolsCount(): Int {
        return selectedTools.values.sumOf { it.size }
    }
}
