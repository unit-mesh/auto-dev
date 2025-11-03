package cc.unitmesh.agent

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.render.DefaultCodingAgentRenderer
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.agent.orchestrator.ToolOrchestrator
import cc.unitmesh.agent.orchestrator.ToolExecutionContext as OrchestratorContext
import cc.unitmesh.agent.parser.ToolCallParser
import cc.unitmesh.agent.policy.DefaultPolicyEngine
import cc.unitmesh.devins.filesystem.EmptyFileSystem
import cc.unitmesh.llm.KoogLLMService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable

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
 */
class CodingAgent(
    private val projectPath: String,
    private val llmService: KoogLLMService,
    maxIterations: Int = 100,
    private val renderer: CodingAgentRenderer = DefaultCodingAgentRenderer()
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
        modelConfig = ModelConfig(
            modelId = "gpt-4",
            temperature = 0.7,
            maxTokens = 2000,
            topP = 1.0
        ),
        runConfig = RunConfig(
            maxTurns = 100,
            maxTimeMinutes = 30,
            terminateOnError = false
        )
    )
), CodingAgentService {

    private val steps = mutableListOf<AgentStep>()
    private val edits = mutableListOf<AgentEdit>()
    private val promptRenderer = CodingAgentPromptRenderer()

    // ToolRegistry for managing file/shell tools
    private val toolRegistry = ToolRegistry(
        fileSystem = DefaultToolFileSystem(projectPath = projectPath),
        shellExecutor = DefaultShellExecutor()
    )

    // New orchestration components
    private val policyEngine = DefaultPolicyEngine()
    private val toolOrchestrator = ToolOrchestrator(toolRegistry, policyEngine, renderer)
    private val toolCallParser = ToolCallParser()

    // SubAgents
    private val errorRecoveryAgent = ErrorRecoveryAgent(projectPath, llmService)
    private val logSummaryAgent = LogSummaryAgent(llmService, threshold = 2000)

    // 上一次恢复结果
    private var lastRecoveryResult: String? = null

    // 重复操作检测
    private val recentToolCalls = mutableListOf<String>()
    private val MAX_REPEAT_COUNT = 3

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
        onProgress("🚀 CodingAgent started")
        onProgress("Project: ${input.projectPath}")
        onProgress("Task: ${input.requirement}")
        
        // 初始化工作空间
        initializeWorkspace(input.projectPath)
        
        // 执行任务
        val result = executeTask(input)
        
        // 返回结果
        return ToolResult.AgentResult(
            success = result.success,
            content = result.message,
            metadata = mapOf(
                "iterations" to currentIteration.toString(),
                "steps" to result.steps.size.toString(),
                "edits" to result.edits.size.toString()
            )
        )
    }

    override suspend fun executeTask(task: AgentTask): AgentResult {
        resetIteration()
        steps.clear()
        edits.clear()

        println("🚀 Starting CodingAgent")
        println("Project: ${task.projectPath}")
        println("Task: ${task.requirement}")

        // 主循环
        while (shouldContinue()) {
            // Check for cancellation
            yield()

            incrementIteration()
            renderer.renderIterationHeader(currentIteration, maxIterations)

            // 1. 构建上下文
            val context = buildContext(task)

            // 2. 生成系统提示
            val systemPrompt = buildSystemPrompt(context)

            // 3. 构建用户提示（包含任务和历史）
            val userPrompt = buildUserPrompt(task, steps)

            // 4. 调用 LLM 获取下一步行动（流式输出）
            val fullPrompt = "$systemPrompt\n\nUser: $userPrompt"
            val llmResponse = StringBuilder()

            try {
                renderer.renderLLMResponseStart()

                // 使用流式输出，支持取消
                llmService.streamPrompt(
                    userPrompt = fullPrompt,
                    fileSystem = EmptyFileSystem(),  // Agent 不需要 DevIns 编译
                    historyMessages = emptyList(),
                    compileDevIns = false  // Agent 已经格式化了 prompt
                ).cancellable().collect { chunk ->
                    llmResponse.append(chunk)
                    renderer.renderLLMResponseChunk(chunk)
                }

                renderer.renderLLMResponseEnd()
            } catch (e: Exception) {
                renderer.renderError("LLM call failed: ${e.message}")
                break
            }

            // 5. 解析所有行动（DevIns 工具调用）
            val toolCalls = toolCallParser.parseToolCalls(llmResponse.toString())

            // 6. 执行所有行动（逐个执行，而不是一次性执行）
            if (toolCalls.isEmpty()) {
                println("✓ No actions needed\n")
                break
            }

            var hasError = false
            for ((index, toolCall) in toolCalls.withIndex()) {
                val toolName = toolCall.toolName

                // 格式化参数为字符串
                val paramsStr = toolCall.params.entries.joinToString(" ") { (key, value) ->
                    "$key=\"$value\""
                }

                // 检测重复操作
                val toolSignature = "$toolName:$paramsStr"
                recentToolCalls.add(toolSignature)
                if (recentToolCalls.size > 10) {
                    recentToolCalls.removeAt(0)
                }

                // 检查最近是否重复调用同一个工具
                val repeatCount = recentToolCalls.takeLast(MAX_REPEAT_COUNT).count { it == toolSignature }

                // 对于任何工具，如果连续2次相同就停止执行
                if (repeatCount >= 2) {
                    renderer.renderRepeatWarning(toolName, repeatCount)
                    println("   Stopping execution due to repeated tool calls")
                    hasError = true
                    break
                }

                // 先显示工具调用
                renderer.renderToolCall(toolName, paramsStr)

                // Check for cancellation before executing tool
                yield()

                // 执行行动 - 使用新的 orchestrator
                val executionContext = OrchestratorContext(
                    workingDirectory = projectPath,
                    environment = emptyMap()
                )
                val executionResult = toolOrchestrator.executeToolCall(
                    toolName,
                    toolCall.params.mapValues { it.value as Any },
                    executionContext
                )

                // 转换为 AgentStep
                val stepResult = AgentStep(
                    step = currentIteration,
                    action = toolName,
                    tool = toolName,
                    params = toolCall.params.mapValues { it.value as Any },
                    result = executionResult.content,
                    success = executionResult.isSuccess
                )
                steps.add(stepResult)

                // 显示工具结果（传递完整输出）
                renderer.renderToolResult(toolName, stepResult.success, stepResult.result, stepResult.result)

                // 如果是 shell 命令失败，自动调用 ErrorRecoveryAgent
                if (!stepResult.success && toolName == "shell") {
                    hasError = true
                    val errorMessage = stepResult.result ?: "Unknown error"

                    // 调用 ErrorRecoveryAgent
                    val recoveryResult = callErrorRecoveryAgent(
                        command = toolCall.params["command"] ?: "",
                        errorMessage = errorMessage
                    )

                    if (recoveryResult != null) {
                        lastRecoveryResult = recoveryResult
                        // 不继续执行后续工具，让 LLM 在下一轮使用恢复建议
                        break
                    }
                }

                // 根据工具类型记录编辑
                if (toolName == "write-file" && executionResult.isSuccess) {
                    val path = toolCall.params["path"]
                    val content = toolCall.params["content"]
                    val mode = toolCall.params["mode"]

                    if (path != null && content != null) {
                        edits.add(AgentEdit(
                            file = path,
                            operation = if (mode == "create") AgentEditOperation.CREATE else AgentEditOperation.UPDATE,
                            content = content
                        ))
                    }
                }
            }

            // 7. 检查是否完成
            if (isTaskComplete(llmResponse.toString())) {
                renderer.renderTaskComplete()
                break
            }

            // 8. 检查是否陷入循环（连续多次无进展）
            if (currentIteration > 5 && steps.takeLast(5).all { !it.success || it.result?.contains("already exists") == true }) {
                renderer.renderError("Agent appears to be stuck. Stopping.")
                break
            }
        }

        val success = steps.any { it.success }
        val message = if (success) {
            "Task completed after $currentIteration iterations"
        } else {
            "Task incomplete after $currentIteration iterations"
        }

        renderer.renderFinalResult(success, message, currentIteration)

        return AgentResult(
            success = success,
            message = message,
            steps = steps,
            edits = edits
        )
    }
    
    /**
     * 构建用户提示（包含任务和最近的历史）
     */
    private fun buildUserPrompt(task: AgentTask, history: List<AgentStep>): String {
        val sb = StringBuilder()
        sb.append("Task: ${task.requirement}\n\n")

        // 检查是否有恢复计划
        if (lastRecoveryResult != null) {
            sb.append("## Previous Action Failed - Recovery Needed\n\n")
            sb.append(lastRecoveryResult!!)
            sb.append("\n\nPlease address the error and continue with the original task.\n\n")
            lastRecoveryResult = null  // 清除恢复结果
        }

        // 添加最近的历史（最后3步）
        if (history.isNotEmpty()) {
            val recentSteps = history.takeLast(3)
            sb.append("Recent history:\n")
            recentSteps.forEach { step ->
                sb.append("- Step ${step.step}: ${step.action}")
                if (step.result != null) {
                    // For read-file, show full content so LLM can see complete file
                    // For other tools, truncate to 200 chars
                    val isReadFile = step.action.contains("/read-file")
                    val maxLength = if (isReadFile) Int.MAX_VALUE else 200
                    val result = if (step.result.length > maxLength) {
                        step.result.take(maxLength) + "..."
                    } else {
                        step.result
                    }
                    sb.append(" -> $result")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append("What should we do next? Use DevIns tools like /read-file, /write-file, /shell, etc.")

        return sb.toString()
    }

    override fun buildSystemPrompt(context: CodingAgentContext, language: String): String {
        return promptRenderer.render(context, language)
    }

    override suspend fun initializeWorkspace(projectPath: String) {
        // TODO: 扫描项目结构，检测构建工具等
    }

    /**
     * 构建上下文
     */
    private fun buildContext(task: AgentTask): CodingAgentContext {
        return CodingAgentContext(
            projectPath = task.projectPath,
            osInfo = getOSInfo(),
            timestamp = getCurrentTimestamp(),
            toolList = getAllTools().joinToString("\n") { it.name }
        )
    }

    /**
     * 获取操作系统信息
     */
    private fun getOSInfo(): String {
        // TODO: 获取实际的 OS 信息
        return "Unknown"
    }

    /**
     * 获取当前时间戳
     */
    private fun getCurrentTimestamp(): String {
        // TODO: 使用跨平台时间API
        return "2024-01-01T00:00:00Z"
    }


    




    /**
     * 调用 ErrorRecoveryAgent 来分析和恢复错误
     */
    private suspend fun callErrorRecoveryAgent(command: String, errorMessage: String): String? {
        println("\n════════════════════════════════════════════════════════")
        println("   🔧 ACTIVATING ERROR RECOVERY SUBAGENT")
        println("════════════════════════════════════════════════════════\n")

        return try {
            val input = mapOf(
                "command" to command,
                "errorMessage" to errorMessage,
                "exitCode" to 1
            )

            val result = errorRecoveryAgent.run(input) { progress ->
                println("   $progress")
            }

            when (result) {
                is ToolResult.AgentResult -> {
                    if (result.success) {
                        println("\n✓ Error Recovery completed")
                        println("Suggestion: ${result.content}\n")
                        result.content
                    } else {
                        println("\n✗ Error Recovery failed: ${result.content}\n")
                        null
                    }
                }
                else -> {
                    println("\n✗ Unexpected result type from ErrorRecoveryAgent\n")
                    null
                }
            }
        } catch (e: Exception) {
            println("\n✗ Error Recovery failed: ${e.message}\n")
            null
        }
    }

    /**
     * 检查任务是否完成
     */
    private fun isTaskComplete(llmResponse: String): Boolean {
        // 检查明确的完成标记
        val completeKeywords = listOf(
            "TASK_COMPLETE",
            "task complete",
            "Task completed",
            "implementation is complete",
            "all done",
            "finished"
        )

        return completeKeywords.any { keyword ->
            llmResponse.contains(keyword, ignoreCase = true)
        }
    }

    // ExecutableTool 抽象方法实现
    
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


