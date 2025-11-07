package cc.unitmesh.agent.config

import cc.unitmesh.agent.mcp.McpServerConfig
import kotlinx.serialization.Serializable

/**
 * Tool Configuration - Manages enabled tools for CodingAgent
 * 
 * Supports both:
 * - Built-in system tools (from ToolType)
 * - MCP (Model Context Protocol) tools from external servers
 * 
 * Stored in ~/.autodev/mcp.json
 */
@Serializable
data class ToolConfigFile(
    /**
     * List of enabled built-in tool names
     * 
     * Available tools:
     * - File System: read-file, write-file, list-files, edit-file, patch-file
     * - Search: grep, glob
     * - Execution: shell
     * - SubAgent: error-recovery, log-summary, codebase-investigator
     */
    val enabledBuiltinTools: List<String> = emptyList(),
    
    /**
     * List of enabled MCP tool names (tool names, not server names)
     */
    val enabledMcpTools: List<String> = emptyList(),
    
    /**
     * MCP Server configurations
     * Maps server name to server config
     */
    val mcpServers: Map<String, McpServerConfig> = emptyMap(),
    
    /**
     * Chat configuration
     */
    val chatConfig: ChatConfig = ChatConfig()
) {
    companion object {
        /**
         * Default configuration with all built-in tools enabled
         */
        fun default(): ToolConfigFile {
            return ToolConfigFile(
                enabledBuiltinTools = listOf(
                    "read-file", "write-file", "list-files", "edit-file", "patch-file",
                    "grep", "glob",
                    "shell",
                    "error-recovery", "log-summary", "codebase-investigator"
                ),
                enabledMcpTools = emptyList(),
                mcpServers = emptyMap(),
                chatConfig = ChatConfig()
            )
        }
    }
}

/**
 * Chat configuration settings
 */
@Serializable
data class ChatConfig(
    val temperature: Double = 0.7,
    val systemPrompt: String = "",
    val maxTokens: Int = 128000
)

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

