package cc.unitmesh.agent.config

import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.ToolCategory

/**
 * Tool Configuration Manager
 *
 * Manages tool configurations including:
 * - Built-in tools (from ToolType)
 * - MCP tools from external servers
 */
object ToolConfigManager {

    /**
     * Get all available built-in tools grouped by category
     */
    fun getBuiltinToolsByCategory(): Map<ToolCategory, List<ToolItem>> {
        val toolsByCategory = mutableMapOf<ToolCategory, MutableList<ToolItem>>()

        ToolType.ALL_TOOLS.forEach { toolType ->
            val category = toolType.category
            val toolItem = ToolItem(
                name = toolType.name,
                displayName = toolType.displayName,
                description = getToolDescription(toolType),
                category = category.name,
                source = ToolSource.BUILTIN,
                schema = null
            )

            toolsByCategory.getOrPut(category) { mutableListOf() }.add(toolItem)
        }

        return toolsByCategory
    }

    /**
     * Get description for a tool type
     */
    private fun getToolDescription(toolType: ToolType): String {
        return when (toolType) {
            ToolType.ReadFile -> "Read file contents from the project"
            ToolType.WriteFile -> "Create or overwrite files in the project"
            ToolType.ListFiles -> "List files and directories"
            ToolType.Grep -> "Search for content in files"
            ToolType.Glob -> "Find files by pattern"
            ToolType.Shell -> "Execute shell commands"
            ToolType.ErrorRecovery -> "Analyze and fix errors automatically"
            ToolType.LogSummary -> "Summarize large log files"
            ToolType.CodebaseInvestigator -> "Investigate codebase structure and dependencies"
        }
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

