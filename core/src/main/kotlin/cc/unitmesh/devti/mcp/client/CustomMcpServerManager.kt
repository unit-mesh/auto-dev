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
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import com.intellij.execution.configurations.GeneralCommandLine

@Service(Service.Level.PROJECT)
class CustomMcpServerManager(val project: Project) {
    val cached = mutableMapOf<String, Map<String, List<Tool>>>()
    val toolClientMap = mutableMapOf<Tool, Client>()

    suspend fun collectServerInfos(): Map<String, List<Tool>> {
        val mcpServerConfig = project.customizeSetting.mcpServerConfig
        if (mcpServerConfig.isEmpty()) return emptyMap()
        if (cached.containsKey(mcpServerConfig)) return cached[mcpServerConfig]!!
        val mcpConfig = McpServer.load(mcpServerConfig)
        if (mcpConfig == null) return emptyMap()

        val toolsMap = mutableMapOf<String, List<Tool>>()
        mcpConfig.mcpServers.forEach { entry ->
            if (entry.value.disabled == true) return@forEach
            val resolvedCommand = resolveCommand(entry.value.command)
            logger<CustomMcpServerManager>().info("Found MCP command: $resolvedCommand")
            val client = Client(clientInfo = Implementation(name = entry.key, version = "1.0.0"))

            val cmd = GeneralCommandLine(resolvedCommand)
            cmd.addParameters(*entry.value.args.toTypedArray())

            entry.value.env?.forEach { (key, value) ->
                cmd.environment[key] = value
            }

            val process = cmd.createProcess()
            val input = process.inputStream.asSource().buffered()
            val output = process.outputStream.asSink().buffered()
            val transport = StdioClientTransport(input, output)

            val tools = try {
                client.connect(transport)
                val listTools = client.listTools()
                listTools?.tools?.forEach { tool ->
                    toolClientMap[tool] = client
                }
                listTools?.tools ?: emptyList()
            } catch (e: Exception) {
                logger<CustomMcpServerManager>().warn("Failed to list tools from ${entry.key}: $e")
                emptyList<Tool>()
            }
            
            toolsMap[entry.key] = tools
        }

        cached[mcpServerConfig] = toolsMap
        return toolsMap
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

