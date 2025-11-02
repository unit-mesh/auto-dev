package cc.unitmesh.agent

import cc.unitmesh.agent.core.MainAgent
import cc.unitmesh.agent.model.*
import cc.unitmesh.agent.subagent.ErrorRecoveryAgent
import cc.unitmesh.agent.subagent.LogSummaryAgent
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolExecutionContext
import cc.unitmesh.agent.tool.ExecutableTool
import cc.unitmesh.agent.tool.ToolErrorType
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.filesystem.DefaultToolFileSystem
import cc.unitmesh.agent.tool.shell.DefaultShellExecutor
import cc.unitmesh.llm.KoogLLMService

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
    maxIterations: Int = 100
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
    
    init {
        // 注册 SubAgents（作为 Tools）
        registerTool(ErrorRecoveryAgent(projectPath, llmService))
        registerTool(LogSummaryAgent(llmService, threshold = 2000))
        
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
            incrementIteration()
            println("\n[$currentIteration/$maxIterations] Analyzing and executing...")
            
            // 1. 构建上下文
            val context = buildContext(task)
            
            // 2. 生成系统提示
            val systemPrompt = buildSystemPrompt(context)
            
            // 3. 构建用户提示（包含任务和历史）
            val userPrompt = buildUserPrompt(task, steps)
            
            // 4. 调用 LLM 获取下一步行动
            val fullPrompt = "$systemPrompt\n\nUser: $userPrompt"
            val llmResponse = try {
                llmService.sendPrompt(fullPrompt)
            } catch (e: Exception) {
                println("❌ LLM call failed: ${e.message}")
                break
            }
            
            println("[LLM Response] ${llmResponse.take(200)}...")
            
            // 5. 检查是否完成
            if (isTaskComplete(llmResponse)) {
                println("✓ Task marked as complete")
                break
            }
            
            // 6. 解析行动（DevIns 工具调用）
            val action = parseAction(llmResponse)
            
            // 7. 执行行动
            val stepResult = executeAction(action)
            steps.add(stepResult)
            
            println("Step result: ${if (stepResult.success) "✓" else "✗"} ${stepResult.action}")
            
            // 8. 如果只是推理，没有工具调用，结束
            if (action.type == "reasoning") {
                println("✓ Agent completed reasoning")
                break
            }
        }
        
        val success = steps.any { it.success }
        val message = if (success) {
            "Task completed after $currentIteration iterations"
        } else {
            "Task incomplete after $currentIteration iterations"
        }
        
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
            sb.append(lastRecoveryResult!!.content)
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
                    sb.append(" -> ${step.result.take(100)}")
                }
                sb.append("\n")
            }
            sb.append("\n")
        }
        
        sb.append("What should we do next? Use DevIns tools like /read-file, /write-file, /shell, etc.")
        
        return sb.toString()
    }
    
    /**
     * 上次恢复结果
     */
    private var lastRecoveryResult: ToolResult.AgentResult? = null

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
     * 解析 LLM 响应中的行动
     * 寻找 DevIns 工具调用，如 /read-file, /write-file, /shell 等
     * 
     * 支持两种格式：
     * 1. 单行格式：/tool-name param1="value1" param2="value2"
     * 2. 多行格式：/tool-name\ncommand content
     */
    private fun parseAction(llmResponse: String): AgentAction {
        // 查找工具调用模式：/tool-name ...
        val toolPattern = Regex("""/(\w+(?:-\w+)*)(.*)""", setOf(RegexOption.MULTILINE))
        val match = toolPattern.find(llmResponse)
        
        if (match != null) {
            val toolName = match.groups[1]?.value ?: return AgentAction("reasoning", null, emptyMap())
            val rest = match.groups[2]?.value?.trim() ?: ""
            
            val params = mutableMapOf<String, Any>()
            
            // 检查是否有 key="value" 格式的参数
            val paramPattern = Regex("""(\w+)="([^"]*)"""")
            val paramMatches = paramPattern.findAll(rest).toList()
            
            if (paramMatches.isNotEmpty()) {
                // 格式 1: /tool key="value" key2="value2"
                paramMatches.forEach { paramMatch ->
                    val key = paramMatch.groups[1]?.value ?: return@forEach
                    val value = paramMatch.groups[2]?.value ?: ""
                    params[key] = value
                }
            } else if (rest.isNotEmpty()) {
                // 格式 2: /shell\ncommand 或 /tool\ncontent
                // 对于 shell 工具，将剩余内容作为 command
                if (toolName == "shell") {
                    // 移除可能的换行符，提取命令
                    val command = rest.trim()
                    if (command.isNotEmpty()) {
                        params["command"] = command
                    }
                } else {
                    // 其他工具：尝试提取第一行作为主要参数
                    val firstLine = rest.lines().firstOrNull()?.trim()
                    if (firstLine != null && firstLine.isNotEmpty()) {
                        // 根据工具类型设置默认参数名
                        val defaultParamName = when (toolName) {
                            "read-file", "write-file" -> "path"
                            "glob", "grep" -> "pattern"
                            else -> "content"
                        }
                        params[defaultParamName] = firstLine
                    }
                }
            }
            
            return AgentAction(
                type = "tool",
                tool = toolName,
                params = params
            )
        }
        
        // 没有找到工具调用，视为推理
        return AgentAction(
            type = "reasoning",
            tool = null,
            params = emptyMap()
        )
    }

    /**
     * Normalize tool parameters to match expected parameter names
     * E.g., "cmd" -> "command" for shell tool
     */
    private fun normalizeToolParams(toolName: String, params: Map<String, Any>): Map<String, Any> {
        return when (toolName) {
            "shell" -> {
                val normalized = params.toMutableMap()
                // Map "cmd" to "command"
                if (normalized.containsKey("cmd") && !normalized.containsKey("command")) {
                    normalized["command"] = normalized["cmd"]!!
                    normalized.remove("cmd")
                }
                normalized
            }
            else -> params
        }
    }

    /**
     * Execute tool with type-specific parameter conversion
     */
    private suspend fun executeToolWithParams(
        tool: cc.unitmesh.agent.tool.Tool,
        toolName: String,
        params: Map<String, Any>,
        context: ToolExecutionContext
    ): ToolResult {
        return when (toolName) {
            "shell" -> {
                val shellTool = tool as cc.unitmesh.agent.tool.impl.ShellTool
                val shellParams = cc.unitmesh.agent.tool.impl.ShellParams(
                    command = params["command"] as? String ?: "",
                    workingDirectory = params["workingDirectory"] as? String,
                    environment = (params["environment"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap(),
                    timeoutMs = (params["timeoutMs"] as? Number)?.toLong() ?: 30000L,
                    description = params["description"] as? String,
                    shell = params["shell"] as? String
                )
                val invocation = shellTool.createInvocation(shellParams)
                invocation.execute(context)
            }
            "read-file" -> {
                val readFileTool = tool as cc.unitmesh.agent.tool.impl.ReadFileTool
                val readFileParams = cc.unitmesh.agent.tool.impl.ReadFileParams(
                    path = params["path"] as? String ?: "",
                    startLine = params["startLine"] as? Int,
                    endLine = params["endLine"] as? Int,
                    maxLines = params["maxLines"] as? Int
                )
                val invocation = readFileTool.createInvocation(readFileParams)
                invocation.execute(context)
            }
            "write-file" -> {
                val writeFileTool = tool as cc.unitmesh.agent.tool.impl.WriteFileTool
                val writeFileParams = cc.unitmesh.agent.tool.impl.WriteFileParams(
                    path = params["path"] as? String ?: "",
                    content = params["content"] as? String ?: "",
                    createDirectories = params["createDirectories"] as? Boolean ?: true,
                    overwrite = params["overwrite"] as? Boolean ?: true,
                    append = params["append"] as? Boolean ?: false
                )
                val invocation = writeFileTool.createInvocation(writeFileParams)
                invocation.execute(context)
            }
            "glob" -> {
                val globTool = tool as cc.unitmesh.agent.tool.impl.GlobTool
                val globParams = cc.unitmesh.agent.tool.impl.GlobParams(
                    pattern = params["pattern"] as? String ?: ""
                )
                val invocation = globTool.createInvocation(globParams)
                invocation.execute(context)
            }
            "grep" -> {
                val grepTool = tool as cc.unitmesh.agent.tool.impl.GrepTool
                val grepParams = cc.unitmesh.agent.tool.impl.GrepParams(
                    pattern = params["pattern"] as? String ?: "",
                    path = params["path"] as? String,
                    include = params["include"] as? String,
                    exclude = params["exclude"] as? String,
                    caseSensitive = params["caseSensitive"] as? Boolean ?: false,
                    maxMatches = params["maxMatches"] as? Int ?: 100,
                    contextLines = params["contextLines"] as? Int ?: 0,
                    recursive = params["recursive"] as? Boolean ?: true
                )
                val invocation = grepTool.createInvocation(grepParams)
                invocation.execute(context)
            }
            else -> {
                ToolResult.Error("Unknown tool: $toolName", ToolErrorType.UNKNOWN.code.toString())
            }
        }
    }

    /**
     * 执行一个行动
     */
    private suspend fun executeAction(action: AgentAction): AgentStep {
        if (action.type == "reasoning") {
            return AgentStep(
                step = currentIteration,
                action = "reasoning",
                tool = null,
                params = null,
                result = "Agent is thinking",
                success = true
            )
        }
        
        val toolName = action.tool ?: return AgentStep(
            step = currentIteration,
            action = "unknown",
            tool = null,
            params = null,
            result = "No tool specified",
            success = false
        )
        
        println("[DEBUG] Executing tool: $toolName with params: ${action.params}")
        
        // Normalize parameters based on tool type
        val normalizedParams = normalizeToolParams(toolName, action.params)
        println("[DEBUG] Normalized params: $normalizedParams")
        
        // 检查工具是否存在
        val tool = toolRegistry.getTool(toolName)
        if (tool == null) {
            val availableTools = toolRegistry.getToolNames().joinToString(", ")
            val errorMsg = "Tool not found: $toolName. Available tools: $availableTools"
            println("[DEBUG] $errorMsg")
            return AgentStep(
                step = currentIteration,
                action = toolName,
                tool = toolName,
                params = action.params,
                result = errorMsg,
                success = false
            )
        }
        
        return try {
            // 创建执行上下文
            val context = ToolExecutionContext(
                workingDirectory = projectPath,
                environment = emptyMap()
            )
            
            // Convert params to tool-specific type and execute
            val result = executeToolWithParams(tool, toolName, normalizedParams, context)
            
            // 根据工具类型记录编辑
            if (toolName == "write-file" && result is ToolResult.Success) {
                val path = action.params["path"] as? String
                val content = action.params["content"] as? String
                val mode = action.params["mode"] as? String
                
                if (path != null && content != null) {
                    edits.add(AgentEdit(
                        file = path,
                        operation = if (mode == "create") AgentEditOperation.CREATE else AgentEditOperation.UPDATE,
                        content = content
                    ))
                }
            }
            
            // 转换为 AgentStep
            AgentStep(
                step = currentIteration,
                action = toolName,
                tool = toolName,
                params = action.params,
                result = when (result) {
                    is ToolResult.Success -> result.content
                    is ToolResult.Error -> result.message
                    is ToolResult.AgentResult -> result.content
                    else -> "Unknown result type"
                },
                success = when (result) {
                    is ToolResult.Success -> true
                    is ToolResult.Error -> false
                    is ToolResult.AgentResult -> result.success
                    else -> false
                }
            )
        } catch (e: Exception) {
            errorStep(toolName, "Tool execution failed: ${e.message}")
        }
    }
    
    /**
     * 创建错误步骤
     */
    private fun errorStep(action: String, message: String): AgentStep {
        return AgentStep(
            step = currentIteration,
            action = action,
            tool = action,
            params = null,
            result = message,
            success = false
        )
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

/**
 * 表示一个 Agent 行动
 */
data class AgentAction(
    val type: String,
    val tool: String?,
    val params: Map<String, Any>
)
