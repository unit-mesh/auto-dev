package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.mcp.model.McpServer
import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Service(Service.Level.PROJECT)
class CustomMcpServerManager(val project: Project) {
    val cached = mutableMapOf<String, Map<String, List<Tool>>>()
    val toolClientMap = mutableMapOf<Tool, Client>()

    val httpClient = HttpClient(CIO) {
        install(SSE) {
            reconnectionTime = 30.seconds
        }
    }

    suspend fun collectServerInfos(): Map<String, List<Tool>> {
        val mcpServerConfig = project.customizeSetting.mcpServerConfig
        if (mcpServerConfig.isEmpty()) return emptyMap()
        if (cached.containsKey(mcpServerConfig)) return cached[mcpServerConfig]!!
        val mcpConfig = McpServer.load(mcpServerConfig)
        if (mcpConfig == null) return emptyMap()

        val toolsMap = mutableMapOf<String, List<Tool>>()
        mcpConfig.mcpServers.forEach { entry ->
            if (entry.value.disabled == true) return@forEach
            val tools = collectServerInfo(entry.key, entry.value)
            toolsMap[entry.key] = tools
        }

        cached[mcpServerConfig] = toolsMap
        return toolsMap
    }

    fun getEnabledServers(content: String): Map<String, McpServer>? {
        val mcpConfig = McpServer.load(content)
        return mcpConfig?.mcpServers?.filter { entry ->
            entry.value.disabled != true
        }?.mapValues { entry ->
            entry.value
        }
    }

    suspend fun collectServerInfo(serverKey: String, serverConfig: McpServer): List<Tool> {
        val client = Client(clientInfo = Implementation(name = serverKey, version = "1.0.0"))

        val transport = when {
            serverConfig.url != null -> {
//                StreamableHttpClientTransport(httpClient, serverConfig.url)
                SseClientTransport(httpClient, serverConfig.url)
            }
            serverConfig.command != null -> {
                val resolvedCommand = resolveCommand(serverConfig.command)
                logger<CustomMcpServerManager>().info("Using stdio transport for $serverKey: $resolvedCommand")

                val cmd = GeneralCommandLine(resolvedCommand)
                cmd.addParameters(*serverConfig.args.toTypedArray())
                cmd.workDirectory = File(project.guessProjectDir()!!.path)

                serverConfig.env?.forEach { (key, value) ->
                    cmd.environment[key] = value
                }

                val process = cmd.createProcess()
                val input = process.inputStream.asSource().buffered()
                val output = process.outputStream.asSink().buffered()
                StdioClientTransport(input, output)
            }
            else -> {
                logger<CustomMcpServerManager>().warn("Server $serverKey has neither command nor url configured, skipping")
                return emptyList()
            }
        }

        return try {
            client.connect(transport)
            val listTools = client.listTools()
            listTools?.tools?.forEach { tool ->
                toolClientMap[tool] = client
            }
            listTools?.tools ?: emptyList()
        } catch (e: Exception) {
            logger<CustomMcpServerManager>().error("Failed to list tools from $serverKey: $e")
            emptyList()
        }
    }

    private val json = Json { prettyPrint = true }

    fun execute(project: Project, tool: Tool, map: String): String {
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

                    val result = it.callTool(tool.name, arguments, true, null)
                    if (result?.content.isNullOrEmpty()) {
                        future.complete("No result from tool ${tool.name}")
                    } else {
                        val result = json.encodeToString(result.content)
                        future.complete(result)
                    }
                } catch (e: Error) {
                    logger<CustomMcpServerManager>().warn("Failed to execute tool ${tool.name}: $e")
                    future.complete("Failed to execute tool ${tool.name}: $e")
                }
            }

            return future.get(30, TimeUnit.SECONDS)
        }

        return "No such tool: ${tool.name} or failed to execute"
    }

    companion object {
        fun instance(project: Project): CustomMcpServerManager {
            return project.getService(CustomMcpServerManager::class.java)
        }
    }
}

