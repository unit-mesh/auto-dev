package cc.unitmesh.agent.mcp

import cc.unitmesh.agent.logging.getLogger
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * MCP Client Manager - Manages connections to MCP servers and tool discovery
 * 
 * This implementation uses Koog's agents-mcp library which provides cross-platform
 * support through the Kotlin MCP SDK. Platform-specific process launching is delegated
 * to McpProcessLauncher expect/actual interface.
 * 
 * Based on:
 * - Koog agents-mcp: Samples/koog/agents/agents-mcp
 * - AutoDev IDEA: core/src/main/kotlin/cc/unitmesh/devti/mcp/client/CustomMcpServerManager.kt
 */
class McpClientManager(
    private val processLauncher: McpProcessLauncher = DefaultMcpProcessLauncher()
) {
    private val logger = getLogger("McpClientManager")
    private val clients = mutableMapOf<String, Client>()
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()
    private val serverTools = mutableMapOf<String, List<McpToolInfo>>()
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private var currentConfig: McpConfig? = null

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Initialize the manager with MCP configuration
     */
    suspend fun initialize(config: McpConfig) {
        currentConfig = config
    }

    /**
     * Discover all tools from all configured MCP servers
     * Returns a map of server name to list of discovered tools
     */
    suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> = withContext(Dispatchers.Default) {
        val config = currentConfig ?: return@withContext emptyMap()

        discoveryState = McpDiscoveryState.IN_PROGRESS
        val result = mutableMapOf<String, List<McpToolInfo>>()

        try {
            val enabledServers = config.getEnabledServers()

            for ((serverName, serverConfig) in enabledServers) {
                try {
                    serverStatuses[serverName] = McpServerStatus.CONNECTING
                    val tools = connectAndDiscoverTools(serverName, serverConfig)
                    result[serverName] = tools
                    serverTools[serverName] = tools
                    serverStatuses[serverName] = McpServerStatus.CONNECTED
                } catch (e: Exception) {
                    logger.error(e) { "Error connecting to MCP server '$serverName': ${e.message}" }
                    serverStatuses[serverName] = McpServerStatus.DISCONNECTED
                }
            }
        } finally {
            discoveryState = McpDiscoveryState.COMPLETED
        }

        result
    }

    /**
     * Connect to an MCP server and discover its tools
     */
    private suspend fun connectAndDiscoverTools(
        serverName: String,
        serverConfig: McpServerConfig
    ): List<McpToolInfo> = withContext(Dispatchers.Default) {
        // Create MCP client
        val client = Client(clientInfo = Implementation(name = "AutoDev-$serverName", version = "1.0.0"))

        // Create transport based on configuration
        val transport = createTransport(serverConfig)

        try {
            // Connect to server
            client.connect(transport)

            // Store client for later use
            clients[serverName] = client

            // List tools
            val listToolsResult = client.listTools()
            val tools = listToolsResult?.tools ?: emptyList()

            // Convert to McpToolInfo
            tools.map { tool ->
                McpToolInfo(
                    name = tool.name,
                    description = tool.description ?: "",
                    serverName = serverName,
                    inputSchema = tool.inputSchema?.let { 
                        // inputSchema is Tool.Input, need to convert to JsonObject first
                        val schemaJson = Json.decodeFromString<JsonObject>(
                            Json.encodeToString(
                                io.modelcontextprotocol.kotlin.sdk.Tool.Input.serializer(),
                                it
                            )
                        )
                        json.encodeToString(JsonObject.serializer(), schemaJson)
                    },
                    enabled = false
                )
            }
        } catch (e: Exception) {
            // Clean up on error
            try {
                client.close()
            } catch (closeError: Exception) {
                // Ignore close errors
            }
            throw e
        }
    }

    /**
     * Create appropriate transport based on server configuration
     */
    private suspend fun createTransport(serverConfig: McpServerConfig): Transport {
        return when {
            serverConfig.isStdioTransport() -> createStdioTransport(serverConfig)
            serverConfig.isNetworkTransport() -> createSseTransport(serverConfig)
            else -> throw IllegalArgumentException("Invalid server configuration: must specify either command or url")
        }
    }

    /**
     * Create stdio transport for process-based MCP servers
     */
    private suspend fun createStdioTransport(serverConfig: McpServerConfig): Transport {
        val command = serverConfig.command!!
        val args = serverConfig.args

        val processConfig = McpProcessConfig(
            command = command,
            args = args,
            workingDirectory = serverConfig.cwd,
            environment = serverConfig.env ?: emptyMap(),
            inheritLoginEnv = true
        )

        return processLauncher.launchStdioProcess(processConfig)
    }

    /**
     * Create SSE transport for HTTP-based MCP servers
     */
    private fun createSseTransport(serverConfig: McpServerConfig): SseClientTransport {
        val url = serverConfig.url!!
        
        // Create HTTP client with SSE support
        val httpClient = io.ktor.client.HttpClient {
            install(io.ktor.client.plugins.sse.SSE)
        }

        return SseClientTransport(
            client = httpClient,
            urlString = url
        )
    }

    /**
     * Discover tools from a specific MCP server
     * Returns a list of discovered tools for the specified server
     */
    suspend fun discoverServerTools(serverName: String): List<McpToolInfo> = withContext(Dispatchers.Default) {
        val config = currentConfig ?: return@withContext emptyList()
        val serverConfig = config.mcpServers[serverName] ?: return@withContext emptyList()

        if (serverConfig.disabled) {
            return@withContext emptyList()
        }

        try {
            return@withContext connectAndDiscoverTools(serverName, serverConfig)
        } catch (e: Exception) {
            logger.error(e) { "Error discovering tools for server '$serverName': ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * Get the current status of a specific MCP server
     */
    fun getServerStatus(serverName: String): McpServerStatus {
        return serverStatuses[serverName] ?: McpServerStatus.DISCONNECTED
    }

    /**
     * Get all server statuses
     */
    fun getAllServerStatuses(): Map<String, McpServerStatus> {
        return serverStatuses.toMap()
    }

    /**
     * Execute a tool on a specific MCP server
     * 
     * @param serverName The name of the MCP server
     * @param toolName The name of the tool to execute
     * @param arguments JSON string of tool arguments
     * @return The result of the tool execution as a JSON string
     */
    suspend fun executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ): String = withContext(Dispatchers.Default) {
        val client = clients[serverName]
            ?: throw IllegalStateException("No client found for server '$serverName'")

        // Parse arguments
        val args = try {
            Json.decodeFromString<JsonObject>(arguments).jsonObject.mapValues { it.value }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid arguments: ${e.message}")
        }

        // Call tool
        val result = client.callTool(toolName, arguments = args, compatibility = true, options = null)

        // Convert result to JSON string
        if (result?.content?.isNotEmpty() == true) {
            json.encodeToString(
                io.modelcontextprotocol.kotlin.sdk.PromptMessageContent.serializer(),
                result.content.first()
            )
        } else {
            ""
        }
    }

    /**
     * Disconnect from all MCP servers and clean up resources
     */
    suspend fun shutdown() = withContext(Dispatchers.Default) {
        for ((serverName, client) in clients) {
            try {
                serverStatuses[serverName] = McpServerStatus.DISCONNECTING
                client.close()
                serverStatuses[serverName] = McpServerStatus.DISCONNECTED
            } catch (e: Exception) {
                logger.error(e) { "Error closing client for server '$serverName': ${e.message}" }
            }
        }
        clients.clear()
        serverTools.clear()
    }

    /**
     * Get discovery state
     */
    fun getDiscoveryState(): McpDiscoveryState {
        return discoveryState
    }
}
