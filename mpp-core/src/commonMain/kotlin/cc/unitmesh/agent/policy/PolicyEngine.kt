package cc.unitmesh.agent.policy

import cc.unitmesh.agent.orchestrator.ToolExecutionContext
import cc.unitmesh.agent.state.ToolCall

/**
 * Interface for tool execution policy engine
 * Responsible for determining whether a tool execution should be allowed
 */
interface PolicyEngine {
    
    /**
     * Check if a tool call should be allowed
     */
    fun checkPermission(
        toolCall: ToolCall, 
        context: ToolExecutionContext
    ): PolicyDecision
    
    /**
     * Get detailed policy decision with reasoning
     */
    fun checkPermissionDetailed(
        toolCall: ToolCall, 
        context: ToolExecutionContext
    ): PolicyDecisionResult
    
    /**
     * Add a policy rule
     */
    fun addRule(rule: PolicyRule)
    
    /**
     * Remove a policy rule by name
     */
    fun removeRule(ruleName: String)
    
    /**
     * Get all policy rules
     */
    fun getRules(): List<PolicyRule>
    
    /**
     * Clear all policy rules
     */
    fun clearRules()
    
    /**
     * Check if a tool is allowed by name
     */
    fun isToolAllowed(toolName: String): Boolean
    
    /**
     * Get risk level for a tool
     */
    fun getToolRiskLevel(toolName: String): RiskLevel
}
