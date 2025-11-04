package cc.unitmesh.agent

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.CodingAgentExecutor
import cc.unitmesh.agent.mcp.McpServerConfig
import cc.unitmesh.agent.mcp.McpToolsInitializer
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.subagent.CodebaseInvestigatorAgent
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.filesystem.ToolFileSystem
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.tool.shell.ShellExecutor
import cc.unitmesh.llm.KoogLLMService
import cc.unitmesh.llm.ModelConfig

class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    override val maxIterations: Int = 100,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer(),
    private val fileSystem: ToolFileSystem? = null,
    private val shellExecutor: ShellExecutor? = null,
    private val mcpServers: Map<String, McpServerConfig>? = null,
    private val toolConfigService: cc.unitmesh.agent.config.ToolConfigService? = null
) : MainAgent<AgentTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "CodingAgent",
        displayName = "Autonomous Coding Agent",
        description = "Autonomous coding agent for development tasks",
        promptConfig = PromptConfig(
            systemPrompt = "You are an autonomous coding agent.",
            queryTemplate = null,
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig.default(),
        runConfig = RunConfig(
            maxTurns = 100,
            maxTimeMinutes = 30,
            terminateOnError = false
        )
    )
), CodingAgentService {

    private val promptRenderer = CodingAgentPromptRenderer()
    
    // Tool configuration service
    private val configService = toolConfigService ?: cc.unitmesh.agent.config.ToolConfigService.default()

    private val toolRegistry = ToolRegistry(
        fileSystem = fileSystem ?: DefaultToolFileSystem(projectPath = projectPath),
        shellExecutor = shellExecutor ?: DefaultShellExecutor()
    )

    private val policyEngine = DefaultPolicyEngine()
    private val toolOrchestrator = ToolOrchestrator(toolRegistry, policyEngine, renderer)

    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService, threshold = 2000)

    private val codebaseInvestigatorAgent = CodebaseInvestigatorAgent(projectPath, llmService)
    
    // MCP Tools 初始化器
    private val mcpToolsInitializer = McpToolsInitializer()

    // 执行器
    private val executor = CodingAgentExecutor(
        projectPath = projectPath,
        llmService = llmService,
        toolOrchestrator = toolOrchestrator,
        renderer = renderer,
        maxIterations = maxIterations
    )

    init {
        // 注册 SubAgents（作为 Tools）- 根据配置决定是否启用
        if (configService.isBuiltinToolEnabled("error-recovery")) {
            registerTool(errorRecoveryAgent)
        }
        if (configService.isBuiltinToolEnabled("log-summary")) {
            registerTool(logSummaryAgent)
        }
        if (configService.isBuiltinToolEnabled("codebase-investigator")) {
            registerTool(codebaseInvestigatorAgent)
        }
    }

    override suspend fun execute(
        input: AgentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        initializeWorkspace(input.projectPath)

        val context = buildContext(input)
        val systemPrompt = buildSystemPrompt(context)

        val result = executor.execute(input, systemPrompt, onProgress)

        return ToolResult.AgentResult(
            success = result.success,
            content = result.message,
            metadata = mapOf(
                "iterations" to "0", // executor 内部管理迭代
                "steps" to result.steps.size.toString(),
                "edits" to result.edits.size.toString()
            )
        )
    }

    override suspend fun executeTask(task: AgentTask): AgentResult {
        // 构建系统提示词
        val context = buildContext(task)
        val systemPrompt = buildSystemPrompt(context)

        // 使用执行器执行任务
        return executor.execute(task, systemPrompt)
    }



    override fun buildSystemPrompt(context: CodingAgentContext, language: String): String {
        return promptRenderer.render(context, language)
    }

    override suspend fun initializeWorkspace(projectPath: String) {
        // Use MCP servers from toolConfigService if available, otherwise fall back to constructor param
        val mcpServersToInit = configService.getEnabledMcpServers().takeIf { it.isNotEmpty() }
            ?: mcpServers
        
        if (!mcpServersToInit.isNullOrEmpty()) {
            initializeMcpTools(mcpServersToInit)
        }
    }
    
    /**
     * Initialize and register MCP tools from configuration
     */
    private suspend fun initializeMcpTools(mcpServers: Map<String, McpServerConfig>) {
        try {
            println("🔌 Initializing MCP tools...")
            
            val mcpTools = mcpToolsInitializer.initialize(mcpServers)
            
            if (mcpTools.isNotEmpty()) {
                // Filter MCP tools based on configuration
                val filteredMcpTools = configService.filterMcpTools(mcpTools)
                
                filteredMcpTools.forEach { tool ->
                    registerTool(tool)
                }
                
                println("✅ Registered ${filteredMcpTools.size}/${mcpTools.size} MCP tools from ${mcpServers.size} servers")
            } else {
                println("ℹ️  No MCP tools discovered")
            }
        } catch (e: Exception) {
            println("⚠️  Warning: Failed to initialize MCP tools: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Shutdown MCP connections
     */
    suspend fun shutdown() {
        mcpToolsInitializer.shutdown()
    }

    private fun buildContext(task: AgentTask): CodingAgentContext {
        return CodingAgentContext.fromTask(
            task,
            toolList = getAllTools()
        )
    }


    override fun validateInput(input: Map<String, Any>): AgentTask {
        val requirement = input["requirement"] as? String
            ?: throw IllegalArgumentException("requirement is required")
        val projectPath = input["projectPath"] as? String
            ?: throw IllegalArgumentException("projectPath is required")

        return AgentTask(requirement, projectPath)
    }

    override fun formatOutput(output: ToolResult.AgentResult): String {
        return output.content
    }
}
