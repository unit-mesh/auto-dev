package cc.unitmesh.agent.config

import cc.unitmesh.agent.mcp.McpServerConfig
import kotlinx.serialization.Serializable

/**
 * Tool Configuration - Manages enabled tools for CodingAgent
 * 
 * Supports both:
 * - Built-in system tools (always enabled, cannot be disabled)
 * - MCP (Model Context Protocol) tools from external servers (configurable)
 * 
 * Stored in ~/.autodev/mcp.json
 */
@Serializable
data class ToolConfigFile(
    /**
     * List of enabled MCP tool names (tool names, not server names)
     */
    val enabledMcpTools: List<String> = emptyList(),
    
    /**
     * MCP Server configurations
     * Maps server name to server config
     */
    val mcpServers: Map<String, McpServerConfig> = emptyMap(),
) {
    companion object {
        /**
         * Default configuration.
         * 
         * Note: Built-in tools are always enabled and don't need to be listed.
         * The enabledBuiltinTools field is deprecated and ignored.
         */
        fun default(): ToolConfigFile {
            return ToolConfigFile(
                enabledMcpTools = emptyList(),
                mcpServers = emptyMap()
            )
        }
    }
}

/**
 * Represents a tool item in the UI
 */
@Serializable
data class ToolItem(
    val name: String,
    val displayName: String,
    val description: String,
    val category: String,
    val source: ToolSource,
    val enabled: Boolean = false,
    /**
     * For MCP tools: the server name
     * For builtin tools: empty string
     */
    val serverName: String = "",
    val schema: String?
)

/**
 * Tool source type
 */
@Serializable
enum class ToolSource {
    BUILTIN,
    MCP
}

