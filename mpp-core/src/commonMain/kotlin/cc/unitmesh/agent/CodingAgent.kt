package cc.unitmesh.agent

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.executor.CodingAgentExecutor
import cc.unitmesh.agent.model.AgentDefinition
import cc.unitmesh.agent.model.PromptConfig
import cc.unitmesh.agent.model.RunConfig
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
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
    private val shellExecutor: ShellExecutor? = null
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

    private val toolRegistry = ToolRegistry(
        fileSystem = fileSystem ?: DefaultToolFileSystem(projectPath = projectPath),
        shellExecutor = shellExecutor ?: DefaultShellExecutor()
    )

    // New orchestration components
    private val policyEngine = DefaultPolicyEngine()
    private val toolOrchestrator = ToolOrchestrator(toolRegistry, policyEngine, renderer)

    // SubAgents
    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService, threshold = 2000)

    // 执行器
    private val executor = CodingAgentExecutor(
        projectPath = projectPath,
        llmService = llmService,
        toolOrchestrator = toolOrchestrator,
        renderer = renderer,
        maxIterations = maxIterations
    )

    init {
        // 注册 SubAgents（作为 Tools）
        registerTool(errorRecoveryAgent)
        registerTool(logSummaryAgent)

        // ToolRegistry 已经在 init 中注册了内置 tools（read-file, write-file, shell, glob）
    }

    override suspend fun execute(
        input: AgentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        // 初始化工作空间
        initializeWorkspace(input.projectPath)

        // 构建系统提示词
        val context = buildContext(input)
        val systemPrompt = buildSystemPrompt(context)

        // 使用执行器执行任务
        val result = executor.execute(input, systemPrompt, onProgress)

        // 返回结果
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
        // TODO: 扫描项目结构，检测构建工具等
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
