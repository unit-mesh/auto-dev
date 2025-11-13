package cc.unitmesh.agent.config

import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.tool.ExecutableTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class McpToolConfigService(val toolConfig: ToolConfigFile) {
    private val logger = getLogger("McpToolConfigService")

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

    fun isMcpToolEnabled(toolName: String): Boolean {
        if (toolConfig.enabledMcpTools.isEmpty()) {
            return false
        }
        return toolName in toolConfig.enabledMcpTools
    }

    fun <T : ExecutableTool<*, *>> filterBuiltinTools(tools: List<T>): List<T> {
        if (toolConfig.enabledBuiltinTools.isEmpty()) {
            return tools
        }

        return tools.filter { tool ->
            isBuiltinToolEnabled(tool.name)
        }
    }

    fun <T : ExecutableTool<*, *>> filterMcpTools(tools: List<T>): List<T> {
        if (toolConfig.enabledMcpTools.isNotEmpty()) {
            logger.info { "   Configured enabled MCP tools:" }
            toolConfig.enabledMcpTools.forEach { toolName ->
                logger.info { "     - $toolName" }
            }
        }

        if (toolConfig.enabledMcpTools.isEmpty()) {
            logger.info { "ℹ️  No MCP tools explicitly enabled, enabling all discovered tools by default" }
            return tools
        }

        val filteredTools = tools.filter { tool ->
            val enabled = isMcpToolEnabled(tool.name)
            logger.debug { "   Tool '${tool.name}': ${if (enabled) "✓ enabled" else "✗ disabled"}" }
            enabled
        }

        logger.info { "🔧 Filtered result: ${filteredTools.size}/${tools.size} tools enabled" }
        return filteredTools
    }

    fun getEnabledMcpServers(): Map<String, cc.unitmesh.agent.mcp.McpServerConfig> {
        return toolConfig.mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
}

