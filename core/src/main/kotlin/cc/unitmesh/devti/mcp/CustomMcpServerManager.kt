package cc.unitmesh.devti.mcp

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.CompletableFuture
import kotlin.text.get

@Serializable
data class McpConfig(
    val mcpServers: Map<String, McpServer>
)

@Serializable
data class McpServer(
    val command: String,
    val args: List<String>,
    val disabled: Boolean? = null,
    val autoApprove: List<String>? = null,
    val env: Map<String, String>? = null,
    val requiresConfirmation: List<String>? = null
) {

    companion object {
        fun load(mcpServerConfig: String): McpConfig? {
            return tryParse(mcpServerConfig)
        }

        fun tryParse(configs: String?): McpConfig? {
            if (configs.isNullOrEmpty()) {
                return null
            }

            try {
                return Json.decodeFromString(configs)
            } catch (e: Exception) {
                logger<McpServer>().warn("Not found mcp config: $e")
            }

            return null
        }
    }
}

@Service(Service.Level.PROJECT)
class CustomMcpServerManager(val project: Project) {
    val cached = mutableMapOf<String, List<Tool>>()
    val toolClientMap = mutableMapOf<Tool, Client>()

    fun collectServerInfos(): List<Tool> {
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

            val future = CompletableFuture<List<Tool>>()

            kotlinx.coroutines.runBlocking {
                try {
                    client.connect(transport)
                    val listTools = client.listTools()
                    if (listTools?.tools != null) {
                        future.complete(listTools.tools)
                    }

                    listTools?.tools?.map {
                        toolClientMap[it] = client
                    }

                    listTools
                } catch (e: java.lang.Error) {
                    logger<CustomMcpServerManager>().warn("Failed to list tools from ${it.key}: $e")
                    null
                }
            }?.tools

            future.get(30, java.util.concurrent.TimeUnit.SECONDS)
        }.flatten()

        cached[mcpServerConfig] = tools
        return tools
    }

    fun execute(project: Project, tool: Tool, map: String): Any {
        toolClientMap[tool]?.let {
            val future = CompletableFuture<Any>()
            kotlinx.coroutines.runBlocking {
                try {
                    val arguments = try {
                        Json.decodeFromString<JsonObject>(map).jsonObject.mapValues { it.value }
                    } catch (e: Exception) {
                        logger<CustomMcpServerManager>().warn("Failed to parse arguments: $e")
                        return@runBlocking future.complete("Invalid arguments: $e")
                    }

                    val result = it.callTool(tool.name, arguments, true, null)
                    future.complete(result)
                } catch (e: Error) {
                    logger<CustomMcpServerManager>().warn("Failed to execute tool ${tool.name}: $e")
                    future.complete("Failed to execute tool ${tool.name}: $e")
                }
            }

            return future.get(30, java.util.concurrent.TimeUnit.SECONDS)
        }

        return "No such tool: ${tool.name} or failed to execute"
    }

    companion object {
        private val logger = logger<CustomMcpServerManager>()

        fun instance(project: Project): CustomMcpServerManager {
            return project.getService(CustomMcpServerManager::class.java)
        }
    }
}

