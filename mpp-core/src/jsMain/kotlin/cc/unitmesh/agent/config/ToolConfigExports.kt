package cc.unitmesh.agent.config

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
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
        if (mcpServers != null && mcpServers != undefined) {
            val keys = js("Object.keys(mcpServers)") as Array<String>
            for (key in keys) {
                val server = mcpServers[key]
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

