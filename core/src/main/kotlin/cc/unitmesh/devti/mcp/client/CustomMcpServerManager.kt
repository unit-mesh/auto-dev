package cc.unitmesh.devti.mcp.client

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class CustomMcpServerManager(val project: Project) {
    val cached = mutableMapOf<String, List<Tool>>()
    val toolClientMap = mutableMapOf<Tool, Client>()

    suspend fun collectServerInfos(): List<Tool> {
        val mcpServerConfig = project.customizeSetting.mcpServerConfig
        if (mcpServerConfig.isEmpty()) {
            return emptyList()
        }

        if (cached.containsKey(mcpServerConfig)) {
            return cached[mcpServerConfig]!!
        }

        val mcpConfig = McpServer.load(mcpServerConfig)
        if (mcpConfig == null) {
            return emptyList()
        }

        val tools: List<Tool> = mcpConfig.mcpServers.mapNotNull { it: Map.Entry<String, McpServer> ->
            if (it.value.disabled == true) {
                return@mapNotNull null
            }

            val client = Client(
                clientInfo = Implementation(
                    name = it.key,
                    version = "1.0.0"
                )
            )

            val processBuilder = ProcessBuilder(it.value.command, *it.value.args.toTypedArray())
            val process = processBuilder.start()

            val input = process.inputStream.asSource().buffered()
            val output = process.outputStream.asSink().buffered()

            val transport = StdioClientTransport(input, output)
            var tools = listOf<Tool>()

            try {
                client.connect(transport)
                val listTools = client.listTools()
                if (listTools?.tools != null) {
                    tools = listTools.tools
                }

                listTools?.tools?.map {
                    toolClientMap[it] = client
                }
            } catch (e: java.lang.Error) {
                logger<CustomMcpServerManager>().warn("Failed to list tools from ${it.key}: $e")
                null
            }

            tools
        }.flatten()

        cached[mcpServerConfig] = tools
        return tools
    }

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
                        val result = Json { prettyPrint = true }.encodeToString(result.content)
                        val text = "Execute ${tool.name} tool's result\n"
                        future.complete(text + result)
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

