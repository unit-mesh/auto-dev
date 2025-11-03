package cc.unitmesh.agent

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.core.DefaultAgentExecutor
import cc.unitmesh.agent.communication.AgentChannel
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.agent.subagent.CodebaseInvestigatorAgent
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolNames
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.devins.workspace.Workspace
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.datetime.Clock

/**
 * CodingAgent - 自动化编码任务的 MainAgent 实现
 *
 * 功能：
 * 1. 分析项目结构
 * 2. 读取和理解代码
 * 3. 根据需求进行代码修改
 * 4. 执行命令和测试
 * 5. 迭代直到任务完成
 *
 * 集成的 Tools：
 * - ReadFileTool: 读取文件内容
 * - WriteFileTool: 写入文件
 * - ShellTool: 执行 shell 命令
 * - GlobTool: 文件搜索
 *
 * 集成的 SubAgents：
 * - ErrorRecoveryAgent: 命令失败时分析和恢复
 * - LogSummaryAgent: 长输出自动摘要
 * - CodebaseInvestigatorAgent: 代码库结构分析和调查
 */
class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    maxIterations: Int = 100,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer(),
    private val channel: AgentChannel? = null
) : MainAgent<AgentTask, ToolResult.AgentResult>(
    AgentDefinition(
        name = "CodingAgent",
        displayName = "Autonomous Coding Agent",
        description = "Autonomous coding agent for development tasks",
        promptConfig = PromptConfig(
            systemPrompt = "You are an autonomous coding agent. Use the available tools to complete development tasks.",
            queryTemplate = "Task: \${requirement}\nProject Path: \${projectPath}",
            initialMessages = emptyList()
        ),
        modelConfig = ModelConfig(
            modelId = "gpt-4",
            temperature = 0.7,
            maxTokens = 2000,
            topP = 1.0
        ),
        runConfig = RunConfig(
            maxTurns = maxIterations,
            maxTimeMinutes = 30,
            terminateOnError = false
        ),
        toolConfig = ToolConfig(
            allowedTools = listOf(
                ToolNames.READ_FILE,
                ToolNames.WRITE_FILE,
                ToolNames.SHELL,
                ToolNames.GLOB,
                ToolNames.ERROR_RECOVERY,
                ToolNames.LOG_SUMMARY,
//                ToolNames.CODEBASE_INVESTIGATOR
            )
        )
    )
), CodingAgentService {

    private val agentExecutor = DefaultAgentExecutor(llmService, channel)

    // ToolRegistry for managing file/shell tools
    private val toolRegistry = ToolRegistry(
        fileSystem = DefaultToolFileSystem(projectPath = projectPath),
        shellExecutor = DefaultShellExecutor()
    )

    // SubAgents
    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService, threshold = 2000)
//    private val codebaseInvestigatorAgent = CodebaseInvestigatorAgent(projectPath, llmService)

    init {
        // 注册 SubAgents（作为 Tools）
        registerTool(errorRecoveryAgent)
        registerTool(logSummaryAgent)
//        registerTool(codebaseInvestigatorAgent)
    }

    override suspend fun execute(
        input: AgentTask,
        onProgress: (String) -> Unit
    ): ToolResult.AgentResult {
        onProgress("🚀 CodingAgent started")
        onProgress("Project: ${input.projectPath}")
        onProgress("Task: ${input.requirement}")

        // 初始化工作空间
        initializeWorkspace(input.projectPath)

        // 创建 Agent 上下文
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val context = AgentContext(
            agentId = "coding-agent-$timestamp",
            sessionId = "session-$timestamp",
            inputs = mapOf(
                "requirement" to input.requirement,
                "projectPath" to input.projectPath
            ),
            projectPath = projectPath,
            metadata = mapOf(
                "projectPath" to projectPath,
                "workspaceInitialized" to "true"
            )
        )

        // 使用 DefaultAgentExecutor 执行
        val result = agentExecutor.execute(
            definition = definition,
            context = context,
            onActivity = { activity ->
                when (activity) {
                    is AgentActivity.Progress -> onProgress(activity.message)
                    is AgentActivity.StreamUpdate -> onProgress(activity.text)
                    is AgentActivity.Error -> onProgress("❌ ${activity.error}")
                    is AgentActivity.ToolCallStart -> onProgress("🔧 ${activity.toolName}")
                    is AgentActivity.ToolCallEnd -> onProgress("✓ ${activity.toolName} completed")
                    is AgentActivity.TaskComplete -> onProgress("✅ Task completed: ${activity.result}")
                    is AgentActivity.ThoughtChunk -> onProgress("💭 ${activity.text}")
                }
            }
        )

        // 转换结果
        return when (result) {
            is AgentResult.Success -> ToolResult.AgentResult(
                success = true,
                content = result.output.toString(),
                metadata = mapOf(
                    "steps" to result.steps.size.toString(),
                    "terminateReason" to "SUCCESS"
                )
            )
            is AgentResult.Failure -> ToolResult.AgentResult(
                success = false,
                content = result.error,
                metadata = mapOf(
                    "steps" to result.steps.size.toString(),
                    "terminateReason" to result.terminateReason.name
                )
            )
        }
    }

    override suspend fun executeTask(task: AgentTask): AgentResult {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val context = AgentContext(
            agentId = "coding-agent-$timestamp",
            sessionId = "session-$timestamp",
            inputs = mapOf(
                "requirement" to task.requirement,
                "projectPath" to task.projectPath
            ),
            projectPath = projectPath,
            metadata = mapOf(
                "projectPath" to projectPath,
                "workspaceInitialized" to "true"
            )
        )

        val result = agentExecutor.execute(
            definition = definition,
            context = context,
            onActivity = { activity ->
                when (activity) {
                    is AgentActivity.Progress -> {
                        println("📊 ${activity.message}")
                        renderer.renderIterationHeader(1, definition.runConfig.maxTurns)
                    }
                    is AgentActivity.StreamUpdate -> {
                        renderer.renderLLMResponseChunk(activity.text)
                    }
                    is AgentActivity.Error -> {
                        println("❌ ${activity.error}")
                        renderer.renderError(activity.error)
                    }
                    is AgentActivity.ToolCallStart -> {
                        println("🔧 ${activity.toolName}")
                        renderer.renderToolCall(activity.toolName, "")
                    }
                    is AgentActivity.ToolCallEnd -> {
                        println("✓ ${activity.toolName} completed")
                    }
                    is AgentActivity.TaskComplete -> {
                        println("✅ Task completed: ${activity.result}")
                        renderer.renderTaskComplete()
                    }
                    is AgentActivity.ThoughtChunk -> {
                        println("💭 ${activity.text}")
                    }
                }
            }
        )

        return when (result) {
            is cc.unitmesh.agent.model.AgentResult.Success -> AgentResult(
                success = true,
                message = result.output.toString(),
                steps = result.steps.map { modelStep ->
                    AgentStep(
                        step = modelStep.step,
                        action = modelStep.action,
                        tool = modelStep.tool,
                        params = modelStep.params,
                        result = modelStep.result,
                        success = modelStep.success
                    )
                },
                edits = emptyList() // TODO: Extract edits from steps
            )
            is cc.unitmesh.agent.model.AgentResult.Failure -> AgentResult(
                success = false,
                message = result.error,
                steps = result.steps.map { modelStep ->
                    AgentStep(
                        step = modelStep.step,
                        action = modelStep.action,
                        tool = modelStep.tool,
                        params = modelStep.params,
                        result = modelStep.result,
                        success = modelStep.success
                    )
                },
                edits = emptyList()
            )
        }
    }

    override fun buildSystemPrompt(context: CodingAgentContext, language: String): String {
        val renderer = CodingAgentPromptRenderer()
        val tools = getAllTools()
        return renderer.renderSystemPrompt(tools, language)
    }

    override suspend fun initializeWorkspace(projectPath: String) {
        println("Initializing workspace at: $projectPath")
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

