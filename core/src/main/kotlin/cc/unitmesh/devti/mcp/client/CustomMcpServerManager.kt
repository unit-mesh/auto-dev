package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.ServerParameters
import io.modelcontextprotocol.client.transport.StdioClientTransport
import io.modelcontextprotocol.spec.McpClientTransport
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


@Service(Service.Level.PROJECT)
class CustomMcpServerManager(val project: Project) {
    val cached = mutableMapOf<String, Map<String, List<McpSchema.Tool>>>()
    val toolClientMap = mutableMapOf<McpSchema.Tool, McpSyncClient>()

    suspend fun collectServerInfos(): Map<String, List<McpSchema.Tool>> {
        val mcpServerConfig = project.customizeSetting.mcpServerConfig
        if (mcpServerConfig.isEmpty()) return emptyMap()
        if (cached.containsKey(mcpServerConfig)) return cached[mcpServerConfig]!!
        val mcpConfig = McpServer.load(mcpServerConfig)
        if (mcpConfig == null) return emptyMap()

        val toolsMap = mutableMapOf<String, List<McpSchema.Tool>>()
        mcpConfig.mcpServers.forEach { entry ->
            if (entry.value.disabled == true) return@forEach
            val resolvedCommand = resolveCommand(entry.value.command)
            logger<CustomMcpServerManager>().info("Found MCP command: $resolvedCommand")

            val params = ServerParameters.builder(entry.value.command)
                .args(entry.value.args)
                .env(entry.value.env)
                .build()

            val transport: McpClientTransport = StdioClientTransport(params)
            var client = McpClient.sync(transport)
                .capabilities(ClientCapabilities.builder()
                    .sampling()
                    .build())
                .build()

            client.initialize()

            val tools = try {
                client.listTools().tools()
            } catch (e: Exception) {
                logger<CustomMcpServerManager>().warn("Failed to list tools: $e")
                return emptyMap()
            }

            tools.forEach {
                toolClientMap[it] = client
            }
            
            toolsMap[entry.key] = tools
        }

        cached[mcpServerConfig] = toolsMap
        return toolsMap
    }

    fun execute(project: Project, tool: McpSchema.Tool, map: String): String {
        toolClientMap[tool]?.let {
            val future = CompletableFuture<String>()
            runBlocking {
                try {
                    val arguments = try {
                        Json.decodeFromString<JsonObject>(map).jsonObject.mapValues { it.value }
                    } catch (e: Exception) {
                        logger<CustomMcpServerManager>().warn("Failed to parse arguments: $e")
                        return@runBlocking future.complete("Invalid arguments: $e")
                    }

                    val request = CallToolRequest(tool.name, arguments)
                    
                    val result = it.callTool(request)
                    if (result?.content.isNullOrEmpty()) {
                        future.complete("No result from tool ${tool.name}")
                    } else {
                        val resultString = Json { prettyPrint = true }.encodeToString(result.content)
                        val text = "Execute ${tool.name} tool's result\n"
                        future.complete(text + resultString)
                    }
                } catch (e: Exception) {
                    logger<CustomMcpServerManager>().warn("Failed to execute tool ${tool.name}: $e")
                    future.complete("Failed to execute tool ${tool.name}: $e")
                }
            }

            return future.get(30, TimeUnit.SECONDS)
        }

        return "No such tool: ${tool.name} or failed to execute"
    }

    fun closeClient(tool: McpSchema.Tool) {
        toolClientMap[tool]?.let {
            runBlocking {
                try {
                    it.closeGracefully()
                    toolClientMap.remove(tool)
                } catch (e: Exception) {
                    logger<CustomMcpServerManager>().warn("Failed to close client for ${tool.name}: $e")
                }
            }
        }
    }

    fun closeAllClients() {
        runBlocking {
            toolClientMap.forEach { (_, client) ->
                try {
                    client.closeGracefully()
                } catch (e: Exception) {
                    logger<CustomMcpServerManager>().warn("Failed to close client: $e")
                }
            }
            toolClientMap.clear()
        }
    }

    companion object {
        fun instance(project: Project): CustomMcpServerManager {
            return project.getService(CustomMcpServerManager::class.java)
        }
    }
}


fun resolveCommand(command: String): String {
    if (SystemInfo.isWindows) {
        try {
            val pb = ProcessBuilder("where", command)
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val resolved = reader.readLine() // take first non-null output
            if (!resolved.isNullOrBlank()) return resolved.trim()
        } catch (e: Exception) {
            logger<CustomMcpServerManager>().warn("Failed to resolve command using where: $e")
        }
    } else {
        val homeDir = System.getProperty("user.home")
        if (command == "npx") {
            val knownPaths = listOf(
                "/opt/homebrew/bin/npx",
                "/usr/local/bin/npx",
                "/usr/bin/npx",
                "$homeDir/.volta/bin/npx",
                "$homeDir/.nvm/current/bin/npx",
                "$homeDir/.npm-global/bin/npx"
            )
            knownPaths.forEach { path ->
                if (File(path).exists()) return path
            }
        }
        try {
            val pb = ProcessBuilder("which", command)
            val currentPath = System.getenv("PATH") ?: ""
            val additionalPaths = if (command == "npx") {
                listOf(
                    "/opt/homebrew/bin",
                    "/opt/homebrew/sbin",
                    "/usr/local/bin",
                    "$homeDir/.volta/bin",
                    "$homeDir/.nvm/current/bin",
                    "$homeDir/.npm-global/bin"
                ).joinToString(":")
            } else ""
            pb.environment()["PATH"] =
                if (additionalPaths.isNotBlank()) "$additionalPaths:$currentPath" else currentPath
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val resolved = reader.readLine()
            if (!resolved.isNullOrBlank()) return resolved.trim()
        } catch (e: Exception) {
            logger<CustomMcpServerManager>().warn("Failed to resolve command using which: $e")
        }
    }
    return command
}
