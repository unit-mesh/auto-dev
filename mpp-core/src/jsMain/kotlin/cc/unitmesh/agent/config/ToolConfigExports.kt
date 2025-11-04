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

    /**
     * Get all built-in tools grouped by category
     */
    @JsName("getBuiltinToolsByCategory")
    fun getBuiltinToolsByCategory(): dynamic {
        val toolsByCategory = ToolConfigManager.getBuiltinToolsByCategory()

        // Convert to JS object
        val result = js("{}")
        toolsByCategory.forEach { (category, tools) ->
            result[category.name] = tools.map { tool ->
                val toolObj = js("{}")
                toolObj["name"] = tool.name
                toolObj["displayName"] = tool.displayName
                toolObj["description"] = tool.description
                toolObj["category"] = tool.category
                toolObj["source"] = tool.source.name
                toolObj["enabled"] = tool.enabled
                toolObj["serverName"] = tool.serverName
                toolObj
            }.toTypedArray()
        }

        return result
    }
    
    /**
     * Get tool configuration summary
     */
    @JsName("getConfigSummary")
    fun getConfigSummary(config: JsToolConfigFile): String {
        return ToolConfigManager.getConfigSummary(config.toCommon())
    }
    
    /**
     * Update tool configuration
     */
    @JsName("updateToolConfig")
    fun updateToolConfig(
        currentConfig: JsToolConfigFile,
        enabledBuiltinTools: Array<String>,
        enabledMcpTools: Array<String>
    ): JsToolConfigFile {
        val updated = ToolConfigManager.updateToolConfig(
            currentConfig.toCommon(),
            enabledBuiltinTools.toList(),
            enabledMcpTools.toList()
        )
        return JsToolConfigFile.fromCommon(updated)
    }
}

/**
 * JS-friendly ToolConfigFile
 */
@JsExport
class JsToolConfigFile(
    val enabledBuiltinTools: Array<String>,
    val enabledMcpTools: Array<String>,
    val mcpServers: dynamic,
    val chatConfig: JsChatConfig
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
            enabledBuiltinTools = enabledBuiltinTools.toList(),
            enabledMcpTools = enabledMcpTools.toList(),
            mcpServers = mcpServersMap,
            chatConfig = chatConfig.toCommon()
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
                enabledBuiltinTools = config.enabledBuiltinTools.toTypedArray(),
                enabledMcpTools = config.enabledMcpTools.toTypedArray(),
                mcpServers = mcpServersJs,
                chatConfig = JsChatConfig.fromCommon(config.chatConfig)
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
        console.log("  Builtin tools:", config.enabledBuiltinTools.size)
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

