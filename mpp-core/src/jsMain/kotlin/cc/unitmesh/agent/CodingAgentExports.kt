package cc.unitmesh.agent

import cc.unitmesh.agent.config.JsToolConfigFile
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.llm.JsMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

@JsExport
data class JsAgentStep(
    val step: Int,
    val action: String,
    val tool: String? = null,
    val params: String? = null,  // JS uses string instead of Any
    val result: String? = null,
    val success: Boolean
) {
    companion object {
        fun fromCommon(step: AgentStep): JsAgentStep {
            return JsAgentStep(
                step = step.step,
                action = step.action,
                tool = step.tool,
                params = step.params?.toString(),
                result = step.result,
                success = step.success
            )
        }
    }
}

/**
 * JS-friendly version of AgentEdit
 */
@JsExport
data class JsAgentEdit(
    val file: String,
    val operation: String,  // Convert enum to string for JS
    val content: String? = null
) {
    companion object {
        fun fromCommon(edit: AgentEdit): JsAgentEdit {
            return JsAgentEdit(
                file = edit.file,
                operation = edit.operation.name.lowercase(),
                content = edit.content
            )
        }
    }
}

/**
 * JS-friendly version of AgentResult
 */
@JsExport
data class JsAgentResult(
    val success: Boolean,
    val message: String,
    val steps: Array<JsAgentStep>,
    val edits: Array<JsAgentEdit>
) {
    companion object {
        fun fromCommon(result: AgentResult): JsAgentResult {
            return JsAgentResult(
                success = result.success,
                message = result.message,
                steps = result.steps.map { JsAgentStep.fromCommon(it) }.toTypedArray(),
                edits = result.edits.map { JsAgentEdit.fromCommon(it) }.toTypedArray()
            )
        }
    }
}

/**
 * JS-friendly version of AgentTask
 */
@JsExport
data class JsAgentTask(
    val requirement: String,
    val projectPath: String
) {
    fun toCommon(): AgentTask {
        return AgentTask(
            requirement = requirement,
            projectPath = projectPath
        )
    }
}

@JsExport
class JsCodingAgent(
    private val projectPath: String,
    private val llmService: cc.unitmesh.llm.JsKoogLLMService,
    private val maxIterations: Int = 100,
    private val renderer: JsCodingAgentRenderer? = null,
    private val mcpServers: dynamic = null,  // JS object for MCP configuration
    private val toolConfig: JsToolConfigFile? = null  // Tool configuration
) {
    // 内部使用 Kotlin 的 CodingAgent
    private val agent: CodingAgent = CodingAgent(
        projectPath = projectPath,
        llmService = llmService.service,  // 访问内部 KoogLLMService
        maxIterations = maxIterations,
        renderer = if (renderer != null) JsRendererAdapter(renderer) else DefaultCodingAgentRenderer(),
        mcpServers = parseMcpServers(mcpServers),
        mcpToolConfigService = createToolConfigService(toolConfig)
    )
    
    /**
     * Create tool config service from JS tool config
     */
    private fun createToolConfigService(jsToolConfig: JsToolConfigFile?): cc.unitmesh.agent.config.McpToolConfigService {
        return if (jsToolConfig != null) {
            cc.unitmesh.agent.config.McpToolConfigService(jsToolConfig.toCommon())
        } else {
            // Create default tool config service with empty configuration
            cc.unitmesh.agent.config.McpToolConfigService(cc.unitmesh.agent.config.ToolConfigFile())
        }
    }

    /**
     * Parse JS MCP servers object to Kotlin map
     */
    private fun parseMcpServers(jsMcpServers: dynamic): Map<String, cc.unitmesh.agent.mcp.McpServerConfig>? {
        if (jsMcpServers == null || jsMcpServers == undefined) {
            return null
        }
        
        return try {
            val map = mutableMapOf<String, cc.unitmesh.agent.mcp.McpServerConfig>()
            val keys = js("Object.keys(jsMcpServers)") as Array<String>
            
            for (key in keys) {
                val server = jsMcpServers[key]
                val config = cc.unitmesh.agent.mcp.McpServerConfig(
                    command = server.command as? String,
                    url = server.url as? String,
                    args = (server.args as? Array<*>)?.map { it.toString() } ?: emptyList(),
                    disabled = (server.disabled as? Boolean) ?: false,
                    autoApprove = (server.autoApprove as? Array<*>)?.map { it.toString() }
                )
                map[key] = config
            }
            
            map
        } catch (e: Exception) {
            console.log("Error parsing MCP servers: ${e.message}")
            null
        }
    }

    /**
     * 执行编码任务
     */
    @JsName("executeTask")
    fun executeTask(task: JsAgentTask): Promise<JsAgentResult> {
        return GlobalScope.promise {
            val kotlinTask = task.toCommon()
            val result = agent.executeTask(kotlinTask)
            JsAgentResult.fromCommon(result)
        }
    }

    /**
     * 初始化工作空间
     */
    @JsName("initializeWorkspace")
    fun initializeWorkspace(): Promise<Unit> {
        return GlobalScope.promise {
            agent.initializeWorkspace(projectPath)
        }
    }

    /**
     * 获取对话历史
     */
    @JsName("getConversationHistory")
    fun getConversationHistory(): Array<JsMessage> {
        val history = agent.getConversationHistory()
        return history.map { msg ->
            JsMessage(msg.role.name.lowercase(), msg.content)
        }.toTypedArray()
    }
}


