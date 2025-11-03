package cc.unitmesh.agent.mcp

import kotlinx.serialization.Serializable

/**
 * MCP Server Configuration
 * 
 * Represents configuration for a single MCP (Model Context Protocol) server.
 * Supports both stdio (command-based) and network (URL-based) transports.
 * 
 * Based on:
 * - AutoDev IDEA implementation: core/src/main/kotlin/cc/unitmesh/devti/agent/extention/McpServer.kt
 * - Gemini CLI implementation: Samples/gemini-cli/packages/core/src/config/config.ts
 */
@Serializable
data class McpServerConfig(
    /**
     * Command to execute for stdio transport (e.g., "npx", "node")
     * Mutually exclusive with url
     */
    val command: String? = null,
    
    /**
     * URL for network transport (SSE or HTTP)
     * Mutually exclusive with command
     */
    val url: String? = null,
    
    /**
     * Command-line arguments for stdio transport
     */
    val args: List<String> = emptyList(),
    
    /**
     * Whether this server is disabled
     */
    val disabled: Boolean = false,
    
    /**
     * Environment variables for stdio transport
     */
    val env: Map<String, String>? = null,
    
    /**
     * Tools that are auto-approved (don't require confirmation)
     */
    val autoApprove: List<String>? = null,
    
    /**
     * Tools that require confirmation before execution
     */
    val requiresConfirmation: List<String>? = null,
    
    /**
     * Timeout in milliseconds for MCP operations
     */
    val timeout: Long? = null,
    
    /**
     * Whether to trust this server (skip confirmation for all tools)
     */
    val trust: Boolean = false,
    
    /**
     * HTTP headers for network transport
     */
    val headers: Map<String, String>? = null,
    
    /**
     * Working directory for stdio transport
     */
    val cwd: String? = null
) {
    /**
     * Check if this is a stdio transport configuration
     */
    fun isStdioTransport(): Boolean = command != null
    
    /**
     * Check if this is a network transport configuration
     */
    fun isNetworkTransport(): Boolean = url != null
    
    /**
     * Validate the configuration
     */
    fun validate(): Boolean {
        // Must have either command or url, but not both
        if (command == null && url == null) return false
        if (command != null && url != null) return false
        return true
    }
}

/**
 * Root MCP configuration containing all server configurations
 */
@Serializable
data class McpConfig(
    /**
     * Map of server name to server configuration
     */
    val mcpServers: Map<String, McpServerConfig> = emptyMap()
) {
    companion object {
        /**
         * Parse MCP configuration from JSON string
         */
        fun fromJson(json: String): McpConfig? {
            return try {
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<McpConfig>(json)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Get all enabled servers
     */
    fun getEnabledServers(): Map<String, McpServerConfig> {
        return mcpServers.filter { !it.value.disabled && it.value.validate() }
    }
}

/**
 * Represents a discovered MCP tool
 */
@Serializable
data class McpToolInfo(
    /**
     * Tool name
     */
    val name: String,
    
    /**
     * Tool description
     */
    val description: String,
    
    /**
     * Server name this tool belongs to
     */
    val serverName: String,
    
    /**
     * JSON schema for tool parameters
     */
    val inputSchema: String? = null,
    
    /**
     * Whether this tool is selected/enabled
     */
    val enabled: Boolean = false
)

/**
 * MCP server connection status
 */
enum class McpServerStatus {
    /** Server is disconnected or experiencing errors */
    DISCONNECTED,
    /** Server is actively disconnecting */
    DISCONNECTING,
    /** Server is in the process of connecting */
    CONNECTING,
    /** Server is connected and ready to use */
    CONNECTED
}

/**
 * MCP discovery state
 */
enum class McpDiscoveryState {
    /** Discovery has not started yet */
    NOT_STARTED,
    /** Discovery is currently in progress */
    IN_PROGRESS,
    /** Discovery has completed (with or without errors) */
    COMPLETED
}

