package cc.unitmesh.agent.mcp


import cc.unitmesh.agent.logging.getLogger
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File

/**
 * JVM implementation of McpClientManager using io.modelcontextprotocol:kotlin-sdk
 *
 * Based on AutoDev IDEA implementation:
 * core/src/main/kotlin/cc/unitmesh/devti/mcp/client/CustomMcpServerManager.kt
 */
actual class McpClientManager {
    private val logger = getLogger("McpClientManager")
    private val clients = mutableMapOf<String, Client>()
    private val serverStatuses = mutableMapOf<String, McpServerStatus>()
    private val toolClientMap = mutableMapOf<Tool, Client>()
    private var discoveryState = McpDiscoveryState.NOT_STARTED
    private var currentConfig: McpConfig? = null

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual suspend fun initialize(config: McpConfig) {
        currentConfig = config
    }

    actual suspend fun discoverAllTools(): Map<String, List<McpToolInfo>> = withContext(Dispatchers.IO) {
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

    private suspend fun connectAndDiscoverTools(
        serverName: String,
        serverConfig: McpServerConfig
    ): List<McpToolInfo> = withContext(Dispatchers.IO) {
        val client = Client(clientInfo = Implementation(name = serverName, version = "1.0.0"))

        // Only support stdio transport for now
        if (!serverConfig.isStdioTransport()) {
            throw IllegalArgumentException("Only stdio transport is supported in JVM implementation")
        }

        val command = serverConfig.command!!
        val args = serverConfig.args

        // Create process
        val processBuilder = ProcessBuilder(listOf(command) + args)

        // Set working directory if specified
        serverConfig.cwd?.let { cwd ->
            processBuilder.directory(File(cwd))
        }

        // Set environment variables
        serverConfig.env?.let { env ->
            processBuilder.environment().putAll(env)
        }

        val process = processBuilder.start()

        // Create transport
        val input = process.inputStream.asSource().buffered()
        val output = process.outputStream.asSink().buffered()
        val transport = StdioClientTransport(input, output)

        try {
            // Connect to server
            client.connect(transport)

            // List tools
            val listToolsResult = client.listTools()
            val tools = listToolsResult?.tools ?: emptyList()

            // Store client for later use
            clients[serverName] = client
            tools.forEach { tool ->
                toolClientMap[tool] = client
            }

            // Convert to McpToolInfo
            tools.map { tool ->
                McpToolInfo(
                    name = tool.name,
                    description = tool.description ?: "",
                    serverName = serverName,
                    inputSchema = tool.inputSchema?.let { json.encodeToString(it) } ?: "",
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

    actual suspend fun discoverServerTools(serverName: String): List<McpToolInfo> = withContext(Dispatchers.IO) {
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

    actual fun getServerStatus(serverName: String): McpServerStatus {
        return serverStatuses[serverName] ?: McpServerStatus.DISCONNECTED
    }

    actual fun getAllServerStatuses(): Map<String, McpServerStatus> {
        return serverStatuses.toMap()
    }

    actual suspend fun executeTool(
        serverName: String,
        toolName: String,
        arguments: String
    ): String = withContext(Dispatchers.IO) {
        val client = clients[serverName]
            ?: throw IllegalStateException("No client found for server '$serverName'")

        // Find the tool
        val tool = toolClientMap.entries.find {
            it.key.name == toolName && it.value == client
        }?.key ?: throw IllegalArgumentException("Tool '$toolName' not found on server '$serverName'")

        // Parse arguments
        val args = try {
            Json.decodeFromString<JsonObject>(arguments).jsonObject.mapValues { it.value }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid arguments: ${e.message}")
        }

        // Call tool
        val result = client.callTool(tool.name, arguments = args,  compatibility = true, options = null)

        if (result?.content?.firstOrNull() != null) {
            json.encodeToString(result.content.first())
        } else {
            ""
        }
    }
    actual suspend fun shutdown() = withContext(Dispatchers.IO) {
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
        toolClientMap.clear()
    }

    actual fun getDiscoveryState(): McpDiscoveryState {
        return discoveryState
    }
}

actual object McpClientManagerFactory {
    actual fun create(): McpClientManager {
        return McpClientManager()
    }
}

