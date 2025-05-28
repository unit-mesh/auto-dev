package cc.unitmesh.devti.mcp.ui

import cc.unitmesh.devti.mcp.client.CustomMcpServerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class McpConfigService(private val project: Project) {
    private val selectedTools = mutableMapOf<String, MutableSet<String>>()

    companion object {
        fun getInstance(project: Project): McpConfigService {
            return project.getService(McpConfigService::class.java)
        }
    }

    fun addSelectedTool(serverName: String, toolName: String) {
        selectedTools.getOrPut(serverName) { mutableSetOf() }.add(toolName)
    }

    fun removeSelectedTool(serverName: String, toolName: String) {
        selectedTools[serverName]?.remove(toolName)
        if (selectedTools[serverName]?.isEmpty() == true) {
            selectedTools.remove(serverName)
        }
    }

    fun isToolSelected(serverName: String, toolName: String): Boolean {
        return selectedTools[serverName]?.contains(toolName) ?: false
    }

    fun getSelectedTools(): Map<String, Set<String>> {
        return selectedTools.mapValues { it.value.toSet() }
    }

    fun clearSelectedTools() {
        selectedTools.clear()
    }
    
    fun setSelectedTools(tools: Map<String, MutableSet<String>>) {
        selectedTools.clear()
        selectedTools.putAll(tools)
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