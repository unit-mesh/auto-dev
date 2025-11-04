package cc.unitmesh.agent.config

import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class McpToolConfigService(private val toolConfig: ToolConfigFile) {
    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            McpToolConfigManager.init(toolConfig)
        }
    }

    fun isBuiltinToolEnabled(toolName: String): Boolean {
        if (toolConfig.enabledBuiltinTools.isEmpty()) {
            return true
        }
        return toolName in toolConfig.enabledBuiltinTools
    }

    fun isBuiltinToolEnabled(toolType: ToolType): Boolean {
        return isBuiltinToolEnabled(toolType.name)
    }

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

    fun <T : ExecutableTool<*, *>> filterMcpTools(tools: List<T>): List<T> {
        if (toolConfig.enabledMcpTools.isNotEmpty()) {
            println("   Configured enabled MCP tools:")
            toolConfig.enabledMcpTools.forEach { toolName ->
                println("     - $toolName")
            }
        }

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

    fun getEnabledMcpServers(): Map<String, cc.unitmesh.agent.mcp.McpServerConfig> {
        return toolConfig.mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
}

