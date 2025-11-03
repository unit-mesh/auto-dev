package cc.unitmesh.agent.orchestrator

import cc.unitmesh.agent.tool.registry.ToolRegistry
import cc.unitmesh.agent.policy.PolicyEngine
import cc.unitmesh.agent.policy.PolicyDecision
import cc.unitmesh.agent.render.CodingAgentRenderer
import cc.unitmesh.agent.state.ToolCall
import cc.unitmesh.agent.state.ToolExecutionState
import cc.unitmesh.agent.state.ToolStateManager
import cc.unitmesh.agent.tool.ToolResult
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
    private val stateManager: ToolStateManager = ToolStateManager()
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
            
            return ToolExecutionResult(
                executionId = context.executionId,
                toolName = toolName,
                result = result,
                startTime = startTime,
                endTime = endTime,
                retryCount = context.currentRetry,
                state = finalState
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
        val tool = registry.getTool(toolName)
            ?: return ToolResult.Error("Tool not found: $toolName")

        // Convert orchestration context to basic tool context
        val basicContext = context.toBasicContext()

        // Execute using the existing tool execution logic from CodingAgent
        return when (toolName) {
            "shell" -> executeShellTool(tool, params, basicContext)
            "read-file" -> executeReadFileTool(tool, params, basicContext)
            "write-file" -> executeWriteFileTool(tool, params, basicContext)
            "glob" -> executeGlobTool(tool, params, basicContext)
            "grep" -> executeGrepTool(tool, params, basicContext)
            else -> ToolResult.Error("Unknown tool: $toolName")
        }
    }

    private suspend fun executeShellTool(
        tool: cc.unitmesh.agent.tool.Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
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
        return invocation.execute(context)
    }

    private suspend fun executeReadFileTool(
        tool: cc.unitmesh.agent.tool.Tool,
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
        tool: cc.unitmesh.agent.tool.Tool,
        params: Map<String, Any>,
        context: cc.unitmesh.agent.tool.ToolExecutionContext
    ): ToolResult {
        val writeFileTool = tool as cc.unitmesh.agent.tool.impl.WriteFileTool
        val writeFileParams = cc.unitmesh.agent.tool.impl.WriteFileParams(
            path = params["path"] as? String ?: "",
            content = params["content"] as? String ?: "",
            createDirectories = params["createDirectories"] as? Boolean ?: true,
            overwrite = params["overwrite"] as? Boolean ?: true,
            append = params["append"] as? Boolean ?: false
        )
        val invocation = writeFileTool.createInvocation(writeFileParams)
        return invocation.execute(context)
    }

    private suspend fun executeGlobTool(
        tool: cc.unitmesh.agent.tool.Tool,
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
        tool: cc.unitmesh.agent.tool.Tool,
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
