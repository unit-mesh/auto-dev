package cc.unitmesh.agent.orchestrator

import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.config.McpToolConfigService
import cc.unitmesh.agent.logging.getLogger
import cc.unitmesh.agent.policy.PolicyDecision
import cc.unitmesh.agent.policy.PolicyEngine
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.impl.WriteFileTool
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.tool.shell.LiveShellExecutor
import cc.unitmesh.agent.tool.shell.LiveShellSession
import cc.unitmesh.agent.tool.shell.ShellExecutionConfig
import cc.unitmesh.agent.tool.shell.ShellExecutor
import kotlinx.coroutines.yield
import kotlinx.datetime.Clock

/**
 * Tool orchestrator responsible for managing tool execution workflow
 * Handles permission checking, state management, and execution coordination
 */
class ToolOrchestrator(
    private val registry: ToolRegistry,
    private val policyEngine: PolicyEngine,
    private val renderer: CodingAgentRenderer,
    private val mcpConfigService: McpToolConfigService? = null
) {
    private val logger = getLogger("ToolOrchestrator")
    
    /**
     * Execute a single tool call with full orchestration
     */
    suspend fun executeToolCall(
        toolName: String,
        params: Map<String, Any>,
        context: ToolExecutionContext
    ): ToolExecutionResult {
        val toolCall = ToolCall.create(toolName, params)
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        try {
            val policyDecision = policyEngine.checkPermission(toolCall, context)
            when (policyDecision) {
                PolicyDecision.DENY -> {
                    val error = "Tool execution denied by policy: $toolName"
                    return ToolExecutionResult.failure(
                        context.executionId, toolName, error, startTime, Clock.System.now().toEpochMilliseconds()
                    )
                }
                PolicyDecision.ASK_USER -> {
                    renderer.renderUserConfirmationRequest(toolName, params)
                }
                PolicyDecision.ALLOW -> {
                    // Continue with execution
                }
            }
            
            yield()
            
            val toolType = toolName.toToolType()
            val isShellTool = toolType == ToolType.Shell
            var liveSession: LiveShellSession? = null
            
            logger.debug { "Checking tool: $toolName, isShellTool: $isShellTool" }
            
            if (isShellTool) {
                // 尝试使用 PTY 执行
                val tool = registry.getTool(toolName)
                logger.debug { "Got tool: ${tool?.let { it::class.simpleName }}" }
                
                if (tool is cc.unitmesh.agent.tool.impl.ShellTool) {
                    val shellExecutor = getShellExecutor(tool)
                    logger.debug { "Shell executor: ${shellExecutor::class.simpleName}" }
                    logger.debug { "Is LiveShellExecutor: ${shellExecutor is LiveShellExecutor}" }
                    
                    if (shellExecutor is LiveShellExecutor) {
                        val supportsLive = shellExecutor.supportsLiveExecution()
                        logger.debug { "Supports live execution: $supportsLive" }
                        
                        if (supportsLive) {
                            // 准备 shell 执行配置
                            val command = params["command"] as? String
                                ?: params["cmd"] as? String
                                ?: return ToolExecutionResult.failure(
                                    context.executionId, toolName, "Shell command cannot be empty", 
                                    startTime, Clock.System.now().toEpochMilliseconds()
                                )
                            
                            logger.debug { "Starting live execution for command: $command" }
                            
                            val shellConfig = ShellExecutionConfig(
                                workingDirectory = params["workingDirectory"] as? String ?: context.workingDirectory,
                                environment = (params["environment"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: context.environment,
                                timeoutMs = (params["timeoutMs"] as? Number)?.toLong() ?: context.timeout,
                                shell = params["shell"] as? String
                            )
                            
                            // 启动 PTY 会话
                            liveSession = shellExecutor.startLiveExecution(command, shellConfig)
                            logger.debug { "Live session started: ${liveSession.sessionId}" }
                            
                            // 立即通知 renderer 添加 LiveTerminal（在执行之前！）
                            logger.debug { "Adding LiveTerminal to renderer" }
                            renderer.addLiveTerminal(
                                sessionId = liveSession.sessionId,
                                command = liveSession.command,
                                workingDirectory = liveSession.workingDirectory,
                                ptyHandle = liveSession.ptyHandle
                            )
                            logger.debug { "LiveTerminal added successfully!" }
                        }
                    }
                }
            }
            
            // Execute the tool (如果已经启动了 PTY，这里需要等待完成)
            val result = if (liveSession != null) {
                // 对于 Live PTY，等待完成并从 session 获取输出
                val shellExecutor = getShellExecutor(registry.getTool(toolName) as cc.unitmesh.agent.tool.impl.ShellTool)
                
                // 等待 PTY 进程完成
                val exitCode = try {
                    if (shellExecutor is LiveShellExecutor) {
                        shellExecutor.waitForSession(liveSession, context.timeout)
                    } else {
                        throw ToolException("Executor does not support live sessions", ToolErrorType.NOT_SUPPORTED)
                    }
                } catch (e: ToolException) {
                    return ToolExecutionResult.failure(
                        context.executionId, toolName, "Command execution error: ${e.message}",
                        startTime, Clock.System.now().toEpochMilliseconds()
                    )
                } catch (e: Exception) {
                    return ToolExecutionResult.failure(
                        context.executionId, toolName, "Command execution error: ${e.message}",
                        startTime, Clock.System.now().toEpochMilliseconds()
                    )
                }
                
                // 从 session 获取输出
                val stdout = liveSession.getStdout()
                val metadata = mapOf(
                    "exit_code" to exitCode.toString(),
                    "execution_time_ms" to (Clock.System.now().toEpochMilliseconds() - startTime).toString(),
                    "shell" to (shellExecutor.getDefaultShell() ?: "unknown"),
                    "stdout" to stdout,
                    "stderr" to ""
                )
                
                if (exitCode == 0) {
                    ToolResult.Success(stdout, metadata)
                } else {
                    ToolResult.Error("Command failed with exit code: $exitCode", metadata = metadata)
                }
            } else {
                // 普通执行
                executeToolInternal(toolName, params, context)
            }
            
            val endTime = Clock.System.now().toEpochMilliseconds()
            
            // Update final state
            val finalState = if (isSuccessResult(result)) {
                ToolExecutionState.Success(toolCall.id, result, endTime - startTime)
            } else {
                val errorMsg = getErrorMessage(result)
                ToolExecutionState.Failed(toolCall.id, errorMsg, endTime - startTime)
            }

            val metadata = result.extractMetadata()
            val finalMetadata = if (liveSession != null) {
                metadata + mapOf("isLiveSession" to "true", "sessionId" to liveSession.sessionId)
            } else {
                metadata
            }
            
            return ToolExecutionResult(
                executionId = context.executionId,
                toolName = toolName,
                result = result,
                startTime = startTime,
                endTime = endTime,
                retryCount = context.currentRetry,
                state = finalState,
                metadata = finalMetadata
            )
            
        } catch (e: Exception) {
            val endTime = Clock.System.now().toEpochMilliseconds()
            val error = "Tool execution failed: ${e.message}"

            return ToolExecutionResult.failure(
                context.executionId, toolName, error, startTime, endTime, context.currentRetry
            )
        }
    }
    
    /**
     * 获取 ShellTool 的执行器
     */
    private fun getShellExecutor(tool: cc.unitmesh.agent.tool.impl.ShellTool): ShellExecutor {
        return tool.getExecutor()
    }

    private suspend fun executeToolInternal(
        toolName: String,
        params: Map<String, Any>,
        context: ToolExecutionContext
    ): ToolResult {
        // First try to get tool from registry (built-in tools)
        val tool = registry.getTool(toolName)
        if (tool != null) {
            // Convert orchestration context to basic tool context
            val basicContext = context.toBasicContext()
            
            // Use new ExecutableTool architecture for most tools
            // Only special-case tools that need custom handling (shell with PTY, etc.)
            return when (val toolType = toolName.toToolType()) {
                ToolType.Shell -> executeShellTool(tool, params, basicContext)
                ToolType.ReadFile -> executeReadFileTool(tool, params, basicContext)
                ToolType.WriteFile -> executeWriteFileTool(tool, params, basicContext)
                ToolType.EditFile -> executeEditFileTool(tool, params, basicContext)
                ToolType.Glob -> executeGlobTool(tool, params, basicContext)
                ToolType.Grep -> executeGrepTool(tool, params, basicContext)
                ToolType.WebFetch -> executeWebFetchTool(tool, params, basicContext)
                ToolType.AskAgent -> executeAskAgentTool(tool, params, basicContext)
                else -> {
                    // Handle special tools that need parameter conversion
                    when (toolName.lowercase()) {
                        "task-boundary" -> executeTaskBoundaryTool(tool, params, basicContext)
                        "docql" -> executeDocQLTool(tool, params, basicContext)
                        else -> {
                            // For truly generic tools, use generic execution
                            logger.debug { "Executing tool generically: $toolName" }
                            executeGenericTool(tool, params, basicContext)
                        }
                    }
                }
            }
        }

        return executeMcpTool(toolName, params, context)
    }

    /**
     * Execute MCP tool by finding the appropriate server and delegating to McpToolConfigManager
     */
    private suspend fun executeMcpTool(
        toolName: String,
        params: Map<String, Any>,
        context: ToolExecutionContext
    ): ToolResult {
        if (mcpConfigService == null) {
            return ToolResult.Error("Tool not found: $toolName (MCP not configured)")
        }

        try {
            val serverName = findMcpServerForTool(toolName)
                ?: return ToolResult.Error("Tool not found: $toolName (no MCP server provides this tool)")

            val arguments = convertParamsToJson(params)

            val result = McpToolConfigManager.executeTool(
                serverName = serverName,
                toolName = toolName,
                arguments = arguments
            )

            return ToolResult.Success(result)

        } catch (e: Exception) {
            return ToolResult.Error("MCP tool execution failed: ${e.message}")
        }
    }

    private suspend fun findMcpServerForTool(toolName: String): String? {
        if (mcpConfigService == null) return null

        try {
            val mcpServers = mcpConfigService.getEnabledMcpServers()
            val enabledMcpTools = mcpConfigService.toolConfig.enabledMcpTools.toSet()

            // Discover tools from all servers to find which one has this tool
            val toolsByServer = McpToolConfigManager.discoverMcpTools(
                mcpServers, enabledMcpTools
            )

            // Find the server that has this tool
            for ((serverName, tools) in toolsByServer) {
                if (tools.any { it.name == toolName && it.enabled }) {
                    return serverName
                }
            }

            return null
        } catch (e: Exception) {
            logger.error(e) { "Error finding MCP server for tool '$toolName': ${e.message}" }
            return null
        }
    }

    /**
     * Convert parameters map to JSON string for MCP
     */
    private fun convertParamsToJson(params: Map<String, Any>): String {
        // Simple JSON conversion - in production you'd use a proper JSON library
        val jsonPairs = params.map { (key, value) ->
            val jsonValue = when (value) {
                is String -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"$value\""
            }
            "\"$key\": $jsonValue"
        }
        return "{${jsonPairs.joinToString(", ")}}"
    }

    private suspend fun executeShellTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val shellTool = tool as cc.unitmesh.agent.tool.impl.ShellTool

        // Extract command - check multiple possible parameter names
        val command = params["command"] as? String
            ?: params["cmd"] as? String
            ?: params.values.firstOrNull { it is String && (it as String).isNotBlank() } as? String
            ?: return ToolResult.Error("Shell command cannot be empty")

        if (command.isBlank()) {
            return ToolResult.Error("Shell command cannot be empty")
        }

        val shellParams = cc.unitmesh.agent.tool.impl.ShellParams(
            command = command,
            workingDirectory = params["workingDirectory"] as? String,
            environment = (params["environment"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap(),
            timeoutMs = (params["timeoutMs"] as? Number)?.toLong() ?: 30000L,
            description = params["description"] as? String,
            shell = params["shell"] as? String
        )
        val invocation = shellTool.createInvocation(shellParams)
        return invocation.execute(context)
    }

    private suspend fun executeReadFileTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val readFileTool = tool as cc.unitmesh.agent.tool.impl.ReadFileTool
        val readFileParams = cc.unitmesh.agent.tool.impl.ReadFileParams(
            path = params["path"] as? String ?: "",
            startLine = params["startLine"] as? Int,
            endLine = params["endLine"] as? Int,
            maxLines = params["maxLines"] as? Int
        )
        val invocation = readFileTool.createInvocation(readFileParams)
        return invocation.execute(context)
    }

    private suspend fun executeWriteFileTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val writeFileTool = tool as WriteFileTool

        val path = params["path"] as? String
        val content = params["content"] as? String

        if (path.isNullOrBlank()) {
            return ToolResult.Error("File path cannot be empty")
        }

        if (content == null) {
            return ToolResult.Error("File content parameter is missing. Please provide the content parameter with the file content to write. Example: /write-file path=\"$path\" content=\"your content here\"")
        }

        // Allow empty content (blank files are valid)
        val actualContent = content

        val writeFileParams = cc.unitmesh.agent.tool.impl.WriteFileParams(
            path = path,
            content = actualContent,
            createDirectories = params["createDirectories"] as? Boolean ?: true,
            overwrite = params["overwrite"] as? Boolean ?: true,
            append = params["append"] as? Boolean ?: false
        )
        val invocation = writeFileTool.createInvocation(writeFileParams)
        return invocation.execute(context)
    }

    private suspend fun executeEditFileTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val editFileTool = tool as cc.unitmesh.agent.tool.impl.EditFileTool

        val filePath = params["filePath"] as? String 
            ?: params["path"] as? String 
            ?: return ToolResult.Error("File path cannot be empty")
        
        val oldString = params["oldString"] as? String 
            ?: params["old_string"] as? String 
            ?: return ToolResult.Error("oldString parameter is required")
        
        val newString = params["newString"] as? String 
            ?: params["new_string"] as? String 
            ?: return ToolResult.Error("newString parameter is required")

        val expectedReplacements = (params["expectedReplacements"] as? Number)?.toInt()
            ?: (params["expected_replacements"] as? Number)?.toInt()
            ?: 1

        val editFileParams = cc.unitmesh.agent.tool.impl.EditFileParams(
            filePath = filePath,
            oldString = oldString,
            newString = newString,
            expectedReplacements = expectedReplacements
        )
        val invocation = editFileTool.createInvocation(editFileParams)
        return invocation.execute(context)
    }

    private suspend fun executeGlobTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val globTool = tool as cc.unitmesh.agent.tool.impl.GlobTool
        val globParams = cc.unitmesh.agent.tool.impl.GlobParams(
            pattern = params["pattern"] as? String ?: ""
        )
        val invocation = globTool.createInvocation(globParams)
        return invocation.execute(context)
    }

    private suspend fun executeGrepTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
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
        return invocation.execute(context)
    }

    private suspend fun executeWebFetchTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val webFetchTool = tool as cc.unitmesh.agent.tool.impl.WebFetchTool
        
        // Handle both prompt-only and prompt+url cases
        // If LLM provides both prompt and url, merge them
        val originalPrompt = params["prompt"] as? String ?: ""
        val url = params["url"] as? String
        
        val finalPrompt = if (url != null && url.isNotBlank()) {
            // If url is provided separately, ensure it's included in the prompt
            if (originalPrompt.contains(url)) {
                originalPrompt
            } else {
                // Prepend the URL to the prompt
                "$originalPrompt $url".trim()
            }
        } else {
            originalPrompt
        }
        
        val webFetchParams = cc.unitmesh.agent.tool.impl.WebFetchParams(
            prompt = finalPrompt
        )
        val invocation = webFetchTool.createInvocation(webFetchParams)
        return invocation.execute(context)
    }

    private suspend fun executeAskAgentTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val askAgentTool = tool as cc.unitmesh.agent.tool.impl.AskAgentTool
        
        val agentName = params["agentName"] as? String
            ?: return ToolResult.Error("agentName parameter is required")
        val question = params["question"] as? String
            ?: return ToolResult.Error("question parameter is required")
        val contextMap = params["context"] as? Map<*, *>
        
        val askAgentParams = cc.unitmesh.agent.tool.impl.AskSubAgentParams(
            agentName = agentName,
            question = question,
            context = contextMap?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
        )
        
        val invocation = askAgentTool.createInvocation(askAgentParams)
        return invocation.execute(context)
    }

    private suspend fun executeTaskBoundaryTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val taskBoundaryTool = tool as cc.unitmesh.agent.tool.impl.TaskBoundaryTool
        
        val taskName = params["taskName"] as? String
            ?: return ToolResult.Error("taskName parameter is required")
        val status = params["status"] as? String
            ?: return ToolResult.Error("status parameter is required")
        val summary = params["summary"] as? String ?: ""
        
        val taskBoundaryParams = cc.unitmesh.agent.tool.impl.TaskBoundaryParams(
            taskName = taskName,
            status = status,
            summary = summary
        )
        
        val invocation = taskBoundaryTool.createInvocation(taskBoundaryParams)
        return invocation.execute(context)
    }

    private suspend fun executeDocQLTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val docqlTool = tool as cc.unitmesh.agent.tool.impl.DocQLTool
        
        val query = params["query"] as? String
            ?: return ToolResult.Error("query parameter is required")
        val documentPath = params["documentPath"] as? String // Optional
        
        val docqlParams = cc.unitmesh.agent.tool.impl.DocQLParams(
            query = query,
            documentPath = documentPath
        )
        
        val invocation = docqlTool.createInvocation(docqlParams)
        return invocation.execute(context)
    }

    private fun isSuccessResult(result: ToolResult): Boolean {
        return when (result) {
            is ToolResult.Success -> true
            is ToolResult.AgentResult -> result.success
            is ToolResult.Error -> false
        }
    }

    private fun getErrorMessage(result: ToolResult): String {
        return when (result) {
            is ToolResult.Error -> result.message
            is ToolResult.AgentResult -> if (!result.success) result.content else ""
            else -> "Unknown error"
        }
    }

    /**
     * Execute generic tool using ExecutableTool interface
     * This handles new tools like task-boundary, ask-agent, etc. without needing specific implementations
     */
    private suspend fun executeGenericTool(
        tool: Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        return try {
            // Cast to ExecutableTool (all new tools should implement this)
            @Suppress("UNCHECKED_CAST")
            val executableTool = tool as? ExecutableTool<Any, ToolResult>
                ?: return ToolResult.Error("Tool ${tool.name} does not implement ExecutableTool interface")

            val invocation = executableTool.createInvocation(params)
            
            // Execute the tool
            invocation.execute(context)
        } catch (e: Exception) {
            logger.error(e) { "Error executing generic tool ${tool.name}" }
            ToolResult.Error("Error executing tool ${tool.name}: ${e.message}")
        }
    }
}
