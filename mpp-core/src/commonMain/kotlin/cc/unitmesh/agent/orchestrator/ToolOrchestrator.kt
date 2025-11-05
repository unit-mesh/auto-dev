package cc.unitmesh.agent.orchestrator

import cc.unitmesh.agent.config.McpToolConfigManager
import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.policy.PolicyEngine
import cc.unitmesh.agent.policy.PolicyDecision
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.state.ToolStateManager
import cc.unitmesh.agent.tool.Tool
import cc.unitmesh.agent.tool.ToolNames
import cc.unitmesh.agent.tool.ToolResult
import cc.unitmesh.agent.tool.ToolType
import cc.unitmesh.agent.tool.toToolType
import cc.unitmesh.agent.tool.impl.WriteFileTool
import cc.unitmesh.agent.config.McpToolConfigService
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
    private val stateManager: ToolStateManager = ToolStateManager(),
    private val mcpConfigService: McpToolConfigService? = null
) {
    
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
        
        // Update state to pending
        stateManager.updateState(ToolExecutionState.Pending(toolCall.id, toolCall))
        
        try {
            // Check permissions
            val policyDecision = policyEngine.checkPermission(toolCall, context)
            when (policyDecision) {
                PolicyDecision.DENY -> {
                    val error = "Tool execution denied by policy: $toolName"
                    stateManager.updateState(ToolExecutionState.Failed(toolCall.id, error, 0))
                    return ToolExecutionResult.failure(
                        context.executionId, toolName, error, startTime, Clock.System.now().toEpochMilliseconds()
                    )
                }
                PolicyDecision.ASK_USER -> {
                    // For now, we'll treat ASK_USER as ALLOW
                    // In a full implementation, this would prompt the user
                    renderer.renderUserConfirmationRequest(toolName, params)
                }
                PolicyDecision.ALLOW -> {
                    // Continue with execution
                }
            }
            
            // Update state to executing
            stateManager.updateState(ToolExecutionState.Executing(toolCall.id, startTime))
            
            // Check for cancellation
            yield()
            
            // Execute the tool
            val result = executeToolInternal(toolName, params, context)
            val endTime = Clock.System.now().toEpochMilliseconds()
            
            // Update final state
            val finalState = if (isSuccessResult(result)) {
                ToolExecutionState.Success(toolCall.id, result, endTime - startTime)
            } else {
                val errorMsg = getErrorMessage(result)
                ToolExecutionState.Failed(toolCall.id, errorMsg, endTime - startTime)
            }
            stateManager.updateState(finalState)
            
            // 从 ToolResult 中提取 metadata
            val metadata = result.extractMetadata()
            
            return ToolExecutionResult(
                executionId = context.executionId,
                toolName = toolName,
                result = result,
                startTime = startTime,
                endTime = endTime,
                retryCount = context.currentRetry,
                state = finalState,
                metadata = metadata
            )
            
        } catch (e: Exception) {
            val endTime = Clock.System.now().toEpochMilliseconds()
            val error = "Tool execution failed: ${e.message}"
            stateManager.updateState(ToolExecutionState.Failed(toolCall.id, error, endTime - startTime))
            
            return ToolExecutionResult.failure(
                context.executionId, toolName, error, startTime, endTime, context.currentRetry
            )
        }
    }
    
    /**
     * Execute a chain of tool calls
     */
    suspend fun executeToolChain(
        calls: List<ToolCall>,
        context: ToolExecutionContext
    ): List<ToolExecutionResult> {
        val results = mutableListOf<ToolExecutionResult>()
        
        for (call in calls) {
            val childContext = context.createChildContext()
            val result = executeToolCall(call.toolName, call.params.mapValues { it.value as Any }, childContext)
            results.add(result)
            
            // Stop execution if a tool fails and no retry is available
            if (!result.isSuccess && !context.canRetry()) {
                break
            }
        }
        
        return results
    }
    
    /**
     * Get current execution state
     */
    fun getExecutionState(callId: String): ToolExecutionState? {
        return stateManager.getState(callId)
    }
    
    /**
     * Get all execution states
     */
    fun getAllExecutionStates(): Map<String, ToolExecutionState> {
        return stateManager.getAllStates()
    }
    
    /**
     * Clear execution history
     */
    fun clearHistory() {
        stateManager.clear()
    }

    /**
     * Internal tool execution - delegates to registry
     */
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
            return when (val toolType = toolName.toToolType()) {
                ToolType.Shell -> executeShellTool(tool, params, basicContext)
                ToolType.ReadFile -> executeReadFileTool(tool, params, basicContext)
                ToolType.WriteFile -> executeWriteFileTool(tool, params, basicContext)
                ToolType.Glob -> executeGlobTool(tool, params, basicContext)
                ToolType.Grep -> executeGrepTool(tool, params, basicContext)
                ToolType.WebFetch -> executeWebFetchTool(tool, params, basicContext)
                else -> ToolResult.Error("Tool not implemented: ${toolType?.displayName ?: "unknown"}")
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
            println("Error finding MCP server for tool '$toolName': ${e.message}")
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
}
