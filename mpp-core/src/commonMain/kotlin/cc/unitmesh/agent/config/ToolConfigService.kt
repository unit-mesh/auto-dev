package cc.unitmesh.agent.config

import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.Tool
import cc.unitmesh.agent.tool.ExecutableTool

/**
 * Service for managing tool configuration
 * 
 * Filters tools based on configuration (enabled/disabled state)
 * Supports both built-in tools and MCP tools
 */
class ToolConfigService(
    private val toolConfig: ToolConfigFile = ToolConfigFile.default()
) {
    /**
     * Check if a built-in tool is enabled
     */
    fun isBuiltinToolEnabled(toolName: String): Boolean {
        // If no specific configuration, enable all by default
        if (toolConfig.enabledBuiltinTools.isEmpty()) {
            return true
        }
        return toolName in toolConfig.enabledBuiltinTools
    }
    
    /**
     * Check if a built-in tool is enabled by ToolType
     */
    fun isBuiltinToolEnabled(toolType: ToolType): Boolean {
        return isBuiltinToolEnabled(toolType.name)
    }
    
    /**
     * Check if an MCP tool is enabled
     */
    fun isMcpToolEnabled(toolName: String): Boolean {
        // If no specific configuration, disable all by default (require explicit enablement)
        if (toolConfig.enabledMcpTools.isEmpty()) {
            return false
        }
        return toolName in toolConfig.enabledMcpTools
    }
    
    /**
     * Filter built-in tools based on configuration
     */
    fun <T : ExecutableTool<*, *>> filterBuiltinTools(tools: List<T>): List<T> {
        // If no configuration, return all tools
        if (toolConfig.enabledBuiltinTools.isEmpty()) {
            return tools
        }
        
        return tools.filter { tool ->
            isBuiltinToolEnabled(tool.name)
        }
    }
    
    /**
     * Filter MCP tools based on configuration
     */
    fun <T : ExecutableTool<*, *>> filterMcpTools(tools: List<T>): List<T> {
        // If no MCP tools enabled, return empty
        if (toolConfig.enabledMcpTools.isEmpty()) {
            return emptyList()
        }
        
        return tools.filter { tool ->
            isMcpToolEnabled(tool.name)
        }
    }
    
    /**
     * Get all enabled built-in tool names
     */
    fun getEnabledBuiltinToolNames(): List<String> {
        return if (toolConfig.enabledBuiltinTools.isEmpty()) {
            ToolType.ALL_TOOLS.map { it.name }
        } else {
            toolConfig.enabledBuiltinTools
        }
    }
    
    /**
     * Get all enabled MCP tool names
     */
    fun getEnabledMcpToolNames(): List<String> {
        return toolConfig.enabledMcpTools
    }
    
    /**
     * Get chat configuration
     */
    fun getChatConfig(): ChatConfig {
        return toolConfig.chatConfig
    }
    
    /**
     * Get enabled MCP servers configuration
     */
    fun getEnabledMcpServers(): Map<String, cc.unitmesh.agent.mcp.McpServerConfig> {
        return toolConfig.mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
    
    companion object {
        /**
         * Create service with default configuration (all builtin tools enabled)
         */
        fun default(): ToolConfigService {
            return ToolConfigService(ToolConfigFile.default())
        }
    }
}

