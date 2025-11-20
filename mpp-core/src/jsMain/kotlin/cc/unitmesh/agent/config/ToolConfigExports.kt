package cc.unitmesh.agent.config

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.serialization.json.Json
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JS exports for Tool Configuration functionality
 * 
 * Provides JavaScript-friendly exports for managing tool configurations
 */

/**
 * JS-friendly tool configuration manager
 */
@JsExport
object JsToolConfigManager {

    /**
     * Load tool configuration from file (~/.autodev/mcp.json)
     */
    @JsName("loadToolConfig")
    fun loadToolConfig(): Promise<JsToolConfigFile> {
        return GlobalScope.promise {
            try {
                val config = loadToolConfigFromFile()
                JsToolConfigFile.fromCommon(config)
            } catch (e: Exception) {
                console.error("Error loading tool config:", e.message)
                JsToolConfigFile.default()
            }
        }
    }

    /**
     * Save tool configuration to file (~/.autodev/mcp.json)
     */
    @JsName("saveToolConfig")
    fun saveToolConfig(config: JsToolConfigFile): Promise<Unit> {
        return GlobalScope.promise {
            try {
                saveToolConfigToFile(config.toCommon())
            } catch (e: Exception) {
                console.error("Error saving tool config:", e.message)
                throw e
            }
        }
    }

    @JsName("getConfigSummary")
    fun getConfigSummary(config: JsToolConfigFile): String {
        val config1 = config.toCommon()
        return buildString {
            this.appendLine("Built-in Tools: Always enabled (all)")
            this.appendLine("MCP Tools: ${config1.enabledMcpTools.size} enabled")
            this.appendLine("MCP Servers: ${config1.mcpServers.size} configured")

            if (config1.enabledMcpTools.isNotEmpty()) {
                this.appendLine("\nEnabled MCP Tools:")
                config1.enabledMcpTools.forEach { toolName ->
                    this.appendLine("  - $toolName")
                }
            }

            if (config1.mcpServers.isNotEmpty()) {
                this.appendLine("\nMCP Servers:")
                config1.mcpServers.forEach { (name, server) ->
                    val status = if (server.disabled) "disabled" else "enabled"
                    this.appendLine("  - $name ($status)")
                }
            }
        }
    }
    
    /**
     * Update tool configuration
     * 
     * @param currentConfig Current configuration
     * @param enabledBuiltinTools Deprecated: Built-in tools are always enabled, this parameter is ignored
     * @param enabledMcpTools List of enabled MCP tool names
     * @return Updated configuration
     */
    @JsName("updateToolConfig")
    fun updateToolConfig(
        currentConfig: JsToolConfigFile,
        enabledBuiltinTools: Array<String>,
        enabledMcpTools: Array<String>
    ): JsToolConfigFile {
        val updated = currentConfig.toCommon().copy(
            enabledMcpTools = enabledMcpTools.toList()
        )
        return JsToolConfigFile.fromCommon(updated)
    }
}

/**
 * JS-friendly ToolConfigFile
 * 
 * Note: enabledBuiltinTools is deprecated and ignored. Built-in tools are always enabled.
 * This field is kept for backward compatibility with existing JS/TS code.
 */
@JsExport
class JsToolConfigFile(
    /** @deprecated Built-in tools are always enabled. This field is ignored. */
    val enabledBuiltinTools: Array<String>,
    val enabledMcpTools: Array<String>,
    val mcpServers: dynamic,
) {
    fun toCommon(): ToolConfigFile {
        // Parse MCP servers from JS object
        val mcpServersMap = mutableMapOf<String, cc.unitmesh.agent.mcp.McpServerConfig>()
        if (this.mcpServers != null && this.mcpServers != undefined) {
            val keys = js("Object.keys(this.mcpServers)") as Array<String>
            for (key in keys) {
                val server = this.mcpServers[key]
                val config = cc.unitmesh.agent.mcp.McpServerConfig(
                    command = server.command as? String,
                    url = server.url as? String,
                    args = (server.args as? Array<*>)?.map { it.toString() } ?: emptyList(),
                    disabled = (server.disabled as? Boolean) ?: false,
                    autoApprove = (server.autoApprove as? Array<*>)?.map { it.toString() }
                )
                mcpServersMap[key] = config
            }
        }
        
        return ToolConfigFile(
            enabledMcpTools = enabledMcpTools.toList(),
            mcpServers = mcpServersMap,
        )
    }
    
    companion object {
        fun fromCommon(config: ToolConfigFile): JsToolConfigFile {
            // Convert MCP servers to JS object
            val mcpServersJs = js("{}")
            config.mcpServers.forEach { (name, server) ->
                val serverObj = js("{}")
                serverObj["command"] = server.command
                serverObj["url"] = server.url
                serverObj["args"] = server.args.toTypedArray()
                serverObj["disabled"] = server.disabled
                serverObj["autoApprove"] = server.autoApprove?.toTypedArray()
                mcpServersJs[name] = serverObj
            }
            
            return JsToolConfigFile(
                enabledBuiltinTools = emptyArray(), // Deprecated: Built-in tools are always enabled
                enabledMcpTools = config.enabledMcpTools.toTypedArray(),
                mcpServers = mcpServersJs
            )
        }
        
        @JsName("default")
        fun default(): JsToolConfigFile {
            return fromCommon(ToolConfigFile.default())
        }
    }
}

/**
 * JS-friendly ChatConfig
 */
@JsExport
class JsChatConfig(
    val temperature: Double,
    val systemPrompt: String,
    val maxTokens: Int
) {
    fun toCommon(): ChatConfig {
        return ChatConfig(
            temperature = temperature,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens
        )
    }
    
    companion object {
        fun fromCommon(config: ChatConfig): JsChatConfig {
            return JsChatConfig(
                temperature = config.temperature,
                systemPrompt = config.systemPrompt,
                maxTokens = config.maxTokens
            )
        }
    }
}

/**
 * Load tool configuration from file system (JS implementation)
 */
private suspend fun loadToolConfigFromFile(): ToolConfigFile {
    return try {
        // Use Node.js fs module (synchronous for simplicity)
        val fs = js("require('fs')")
        val os = js("require('os')")
        val path = js("require('path')")

        val homeDir = os.homedir() as String
        val configDir = path.join(homeDir, ".autodev") as String
        val configFile = path.join(configDir, "mcp.json") as String

        console.log("🔍 Loading tool config from:", configFile)

        // Check if file exists
        val fileExists = try {
            fs.existsSync(configFile) as Boolean
        } catch (e: dynamic) {
            false
        }

        console.log("📁 File exists:", fileExists)

        if (!fileExists) {
            console.log("⚠️ Tool config file doesn't exist, using default")
            return ToolConfigFile.default()
        }

        console.log("✅ Tool config file exists")

        // Read file content synchronously
        val content = fs.readFileSync(configFile, "utf-8") as String
        console.log("📄 Tool config file content length:", content.length)
        console.log("📄 Tool config file content preview:", content.take(200))

        // Parse JSON
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        val config = json.decodeFromString<ToolConfigFile>(content)
        console.log("✅ Tool config parsed successfully")
        console.log("  Built-in tools: Always enabled (all)")
        console.log("  MCP tools:", config.enabledMcpTools.size)
        console.log("  MCP servers:", config.mcpServers.size)

        config
    } catch (e: Exception) {
        console.error("❌ Error loading tool config from file:", e.message)
        console.error("Stack trace:", e.stackTraceToString())
        ToolConfigFile.default()
    }
}

/**
 * Save tool configuration to file system (JS implementation)
 */
private suspend fun saveToolConfigToFile(config: ToolConfigFile) {
    try {
        // Use Node.js fs module (synchronous for simplicity)
        val fs = js("require('fs')")
        val os = js("require('os')")
        val path = js("require('path')")

        val homeDir = os.homedir() as String
        val configDir = path.join(homeDir, ".autodev") as String
        val configFile = path.join(configDir, "mcp.json") as String

        // Ensure directory exists
        try {
            fs.mkdirSync(configDir, js("{ recursive: true }"))
        } catch (e: dynamic) {
            // Directory might already exist
        }

        // Serialize to JSON
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        val jsonContent = json.encodeToString(ToolConfigFile.serializer(), config)

        // Write file synchronously
        fs.writeFileSync(configFile, jsonContent, "utf-8")
    } catch (e: Exception) {
        console.error("Error saving tool config to file:", e.message)
        throw e
    }
}

