package cc.unitmesh.devti.mcp

import cc.unitmesh.devti.settings.customize.customizeSetting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.util.concurrent.CompletableFuture

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
        fun load(): McpConfig? {
            val project = ProjectManager.getInstance().openProjects.first()
            return tryParse(project.customizeSetting.mcpServerConfig)
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
    fun collectServerInfos(): List<Any> {
        val mcpConfig = McpServer.load()
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

                    listTools
                } catch (e: java.lang.Error) {
                    logger<CustomMcpServerManager>().warn("Failed to list tools from ${it.key}: $e")
                    null
                }
            }?.tools

            future.get(30, java.util.concurrent.TimeUnit.SECONDS)
        }.flatten()


        return tools
    }

    companion object {
        private val logger = logger<CustomMcpServerManager>()

        fun instance(project: Project): CustomMcpServerManager {
            return project.getService(CustomMcpServerManager::class.java)
        }
    }
}