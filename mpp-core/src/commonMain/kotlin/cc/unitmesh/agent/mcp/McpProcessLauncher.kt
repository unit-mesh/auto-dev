package cc.unitmesh.agent.mcp

import io.modelcontextprotocol.kotlin.sdk.shared.Transport

/**
 * Configuration for launching an MCP server process
 */
data class McpProcessConfig(
    /**
     * Command to execute (e.g., "npx", "node", "python")
     */
    val command: String,
    
    /**
     * Command-line arguments
     */
    val args: List<String> = emptyList(),
    
    /**
     * Working directory
     */
    val workingDirectory: String? = null,
    
    /**
     * Environment variables to set
     */
    val environment: Map<String, String> = emptyMap(),
    
    /**
     * Whether to inherit login shell environment (PATH, etc.)
     * Default is true to ensure tools like npx, node work correctly
     */
    val inheritLoginEnv: Boolean = true
)

/**
 * Cross-platform process launcher for MCP servers
 * 
 * This interface abstracts the process launching functionality to support
 * different platforms. Each platform provides its own implementation:
 * - JVM: Uses ProcessBuilder with shell environment support
 * - Other platforms: Can provide their own implementations
 */
interface McpProcessLauncher {
    /**
     * Launch an MCP server process and create a transport for communication
     * 
     * @param config Process configuration including command, args, environment
     * @return Transport for communicating with the MCP server process
     */
    suspend fun launchStdioProcess(config: McpProcessConfig): Transport
    
    /**
     * Check if process launching is supported on this platform
     */
    fun isSupported(): Boolean
}

/**
 * Platform-specific process launcher implementation
 */
expect class DefaultMcpProcessLauncher() : McpProcessLauncher {
    override suspend fun launchStdioProcess(config: McpProcessConfig): Transport
    override fun isSupported(): Boolean
}
