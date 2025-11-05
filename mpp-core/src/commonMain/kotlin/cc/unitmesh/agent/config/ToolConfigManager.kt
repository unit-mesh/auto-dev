package cc.unitmesh.agent.config

import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolCategory

/**
 * Tool Configuration Manager
 *
 * Manages tool configurations including:
 * - Built-in tools (self-describing via metadata)
 * - MCP tools from external servers
 * 
 * Note: Tools are now self-describing. This manager no longer needs
 * to hardcode tool information or maintain parallel metadata.
 */
object ToolConfigManager {

    /**
     * Get all available built-in tools grouped by category
     * 
     * @param tools List of executable tools to organize by category
     * @return Tools grouped by category
     */
    fun getBuiltinToolsByCategory(
        tools: List<ExecutableTool<*, *>>
    ): Map<ToolCategory, List<ToolItem>> {
        val toolsByCategory = mutableMapOf<ToolCategory, MutableList<ToolItem>>()

        tools.forEach { tool ->
            val metadata = tool.metadata
            val category = metadata.category
            
            val toolItem = ToolItem(
                name = tool.name,
                displayName = metadata.displayName,
                description = tool.description,
                category = category.name,
                source = ToolSource.BUILTIN,
                schema = null
            )

            toolsByCategory.getOrPut(category) { mutableListOf() }.add(toolItem)
        }

        return toolsByCategory
    }

    /**
     * Create a tool configuration from current config file
     */
    fun applyEnabledTools(
        toolsByCategory: Map<ToolCategory, List<ToolItem>>,
        config: ToolConfigFile
    ): Map<ToolCategory, List<ToolItem>> {
        val enabledBuiltinTools = config.enabledBuiltinTools.toSet()

        return toolsByCategory.mapValues { (_, tools) ->
            tools.map { tool ->
                tool.copy(enabled = tool.name in enabledBuiltinTools)
            }
        }
    }

    /**
     * Discover MCP tools from server configurations
     */
    suspend fun discoverMcpTools(
        mcpServers: Map<String, McpServerConfig>,
        enabledMcpTools: Set<String>
    ): Map<String, List<ToolItem>> {
        return McpToolConfigManager.discoverMcpTools(mcpServers, enabledMcpTools)
    }

    fun updateToolConfig(
        currentConfig: ToolConfigFile,
        enabledBuiltinTools: List<String>,
        enabledMcpTools: List<String>
    ): ToolConfigFile {
        return currentConfig.copy(
            enabledBuiltinTools = enabledBuiltinTools,
            enabledMcpTools = enabledMcpTools
        )
    }

    fun getConfigSummary(config: ToolConfigFile): String {
        return buildString {
            appendLine("Built-in Tools: ${config.enabledBuiltinTools.size} enabled")
            appendLine("MCP Tools: ${config.enabledMcpTools.size} enabled")
            appendLine("MCP Servers: ${config.mcpServers.size} configured")

            if (config.enabledBuiltinTools.isNotEmpty()) {
                appendLine("\nEnabled Built-in Tools:")
                config.enabledBuiltinTools.forEach { toolName ->
                    appendLine("  - $toolName")
                }
            }

            if (config.enabledMcpTools.isNotEmpty()) {
                appendLine("\nEnabled MCP Tools:")
                config.enabledMcpTools.forEach { toolName ->
                    appendLine("  - $toolName")
                }
            }

            if (config.mcpServers.isNotEmpty()) {
                appendLine("\nMCP Servers:")
                config.mcpServers.forEach { (name, server) ->
                    val status = if (server.disabled) "disabled" else "enabled"
                    appendLine("  - $name ($status)")
                }
            }
        }
    }
}

