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
        println("🔍 Filtering MCP tools: ${tools.size} discovered, ${toolConfig.enabledMcpTools.size} configured")

        // Debug: Print configured enabled tools
        if (toolConfig.enabledMcpTools.isNotEmpty()) {
            println("   Configured enabled MCP tools:")
            toolConfig.enabledMcpTools.forEach { toolName ->
                println("     - $toolName")
            }
        }

        // If no MCP tools configuration exists, enable all discovered tools by default
        // This allows MCP tools to work out of the box when servers are configured
        if (toolConfig.enabledMcpTools.isEmpty()) {
            println("ℹ️  No MCP tools explicitly enabled, enabling all discovered tools by default")
            return tools
        }

        val filteredTools = tools.filter { tool ->
            val enabled = isMcpToolEnabled(tool.name)
            println("   Tool '${tool.name}': ${if (enabled) "✓ enabled" else "✗ disabled"}")
            enabled
        }

        println("🔧 Filtered result: ${filteredTools.size}/${tools.size} tools enabled")
        return filteredTools
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

