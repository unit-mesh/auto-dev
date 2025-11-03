package cc.unitmesh.agent

import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.Promise

/**
 * JS exports for Coding Agent functionality
 * 
 * This file provides JavaScript-friendly exports of the coding agent
 * functionality for use in Node.js/browser environments
 */

/**
 * JS-friendly version of CodingAgentContext
 */
@JsExport
data class JsCodingAgentContext(
    val currentFile: String? = null,
    val projectPath: String,
    val projectStructure: String = "",
    val osInfo: String,
    val timestamp: String,
    val toolList: String = "",
    val agentRules: String = "",
    val buildTool: String = "",
    val shell: String = "/bin/bash",
    val moduleInfo: String = "",
    val frameworkContext: String = "",
) {
    /**
     * Convert to common CodingAgentContext
     */
    fun toCommon(): CodingAgentContext {
        return CodingAgentContext(
            currentFile = currentFile,
            projectPath = projectPath,
            projectStructure = projectStructure,
            osInfo = osInfo,
            timestamp = timestamp,
            toolList = toolList,
            agentRules = agentRules,
            buildTool = buildTool,
            shell = shell,
            moduleInfo = moduleInfo,
            frameworkContext = frameworkContext
        )
    }
    
    companion object {
        /**
         * Create from common CodingAgentContext
         */
        fun fromCommon(context: CodingAgentContext): JsCodingAgentContext {
            return JsCodingAgentContext(
                currentFile = context.currentFile,
                projectPath = context.projectPath,
                projectStructure = context.projectStructure,
                osInfo = context.osInfo,
                timestamp = context.timestamp,
                toolList = context.toolList,
                agentRules = context.agentRules,
                buildTool = context.buildTool,
                shell = context.shell,
                moduleInfo = context.moduleInfo,
                frameworkContext = context.frameworkContext
            )
        }
    }
}

/**
 * JS-friendly version of AgentStep
 */
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

/**
 * JS-friendly Prompt Renderer
 */
@JsExport
class JsCodingAgentPromptRenderer {
    private val renderer = CodingAgentPromptRenderer()
    
    /**
     * Render system prompt from context
     */
    fun render(context: JsCodingAgentContext, language: String = "EN"): String {
        return renderer.render(context.toCommon(), language)
    }
}

/**
 * Context builder for JavaScript
 * This provides a convenient way to build context from JS
 */
@JsExport
class JsCodingAgentContextBuilder {
    private var currentFile: String? = null
    private var projectPath: String = ""
    private var projectStructure: String = ""
    private var osInfo: String = ""
    private var timestamp: String = ""
    private var toolList: String = ""
    private var agentRules: String = ""
    private var buildTool: String = ""
    private var shell: String = "/bin/bash"
    private var moduleInfo: String = ""
    private var frameworkContext: String = ""
    
    fun setCurrentFile(value: String?) = apply { this.currentFile = value }
    fun setProjectPath(value: String) = apply { this.projectPath = value }
    fun setProjectStructure(value: String) = apply { this.projectStructure = value }
    fun setOsInfo(value: String) = apply { this.osInfo = value }
    fun setTimestamp(value: String) = apply { this.timestamp = value }
    fun setToolList(value: String) = apply { this.toolList = value }
    fun setAgentRules(value: String) = apply { this.agentRules = value }
    fun setBuildTool(value: String) = apply { this.buildTool = value }
    fun setShell(value: String) = apply { this.shell = value }
    fun setModuleInfo(value: String) = apply { this.moduleInfo = value }
    fun setFrameworkContext(value: String) = apply { this.frameworkContext = value }
    
    fun build(): JsCodingAgentContext {
        return JsCodingAgentContext(
            currentFile = currentFile,
            projectPath = projectPath,
            projectStructure = projectStructure,
            osInfo = osInfo,
            timestamp = timestamp,
            toolList = toolList,
            agentRules = agentRules,
            buildTool = buildTool,
            shell = shell,
            moduleInfo = moduleInfo,
            frameworkContext = frameworkContext
        )
    }
}

/**
 * JS Export for CodingAgent (MainAgent)
 * 使用 Kotlin 的 MainAgent 替代 TypeScript 的 CodingAgentService
 */
@JsExport
class JsCodingAgent(
    private val projectPath: String,
    private val llmService: cc.unitmesh.llm.JsKoogLLMService,
    private val maxIterations: Int = 100,
    private val renderer: JsCodingAgentRenderer? = null,
    private val mcpServers: dynamic = null  // JS object for MCP configuration
) {
    // 内部使用 Kotlin 的 CodingAgent
    private val agent: CodingAgent = CodingAgent(
        projectPath = projectPath,
        llmService = llmService.service,  // 访问内部 KoogLLMService
        maxIterations = maxIterations,
        renderer = if (renderer != null) JsRendererAdapter(renderer) else DefaultCodingAgentRenderer(),
        mcpServers = parseMcpServers(mcpServers)
    )
    
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
}


