package cc.unitmesh.agent.policy

import cc.unitmesh.agent.orchestrator.ToolExecutionContext
import cc.unitmesh.agent.state.ToolCall

/**
 * Default implementation of PolicyEngine
 * Provides basic security policies for common tools
 */
class DefaultPolicyEngine : PolicyEngine {
    private val rules = mutableListOf<PolicyRule>()
    
    init {
        // Initialize with default rules
        initializeDefaultRules()
    }
    
    override fun checkPermission(toolCall: ToolCall, context: ToolExecutionContext): PolicyDecision {
        return checkPermissionDetailed(toolCall, context).decision
    }
    
    override fun checkPermissionDetailed(
        toolCall: ToolCall, 
        context: ToolExecutionContext
    ): PolicyDecisionResult {
        // Sort rules by priority (higher first)
        val sortedRules = rules.sortedByDescending { it.priority }
        
        for (rule in sortedRules) {
            if (matchesRule(toolCall, rule)) {
                return PolicyDecisionResult(
                    decision = rule.decision,
                    reason = "Matched rule: ${rule.name} - ${rule.description}",
                    riskLevel = rule.riskLevel,
                    metadata = mapOf(
                        "rule_name" to rule.name,
                        "tool_name" to toolCall.toolName
                    )
                )
            }
        }
        
        // Default: allow with low risk
        return PolicyDecisionResult(
            decision = PolicyDecision.ALLOW,
            reason = "No matching rules found, using default allow policy",
            riskLevel = RiskLevel.LOW
        )
    }
    
    override fun addRule(rule: PolicyRule) {
        rules.add(rule)
    }
    
    override fun removeRule(ruleName: String) {
        rules.removeAll { it.name == ruleName }
    }
    
    override fun getRules(): List<PolicyRule> {
        return rules.toList()
    }
    
    override fun clearRules() {
        rules.clear()
    }
    
    override fun isToolAllowed(toolName: String): Boolean {
        val dummyCall = ToolCall.create(toolName, emptyMap())
        val dummyContext = ToolExecutionContext()
        return checkPermission(dummyCall, dummyContext) != PolicyDecision.DENY
    }
    
    override fun getToolRiskLevel(toolName: String): RiskLevel {
        val dummyCall = ToolCall.create(toolName, emptyMap())
        val dummyContext = ToolExecutionContext()
        return checkPermissionDetailed(dummyCall, dummyContext).riskLevel
    }
    
    /**
     * Check if a tool call matches a policy rule
     */
    private fun matchesRule(toolCall: ToolCall, rule: PolicyRule): Boolean {
        // Check tool name pattern
        val toolNameRegex = Regex(rule.toolPattern)
        if (!toolNameRegex.matches(toolCall.toolName)) {
            return false
        }
        
        // Check parameter rules
        for (paramRule in rule.parameterRules) {
            if (!matchesParameterRule(toolCall, paramRule)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Check if a tool call matches a parameter rule
     */
    private fun matchesParameterRule(toolCall: ToolCall, paramRule: ParameterRule): Boolean {
        val paramValue = toolCall.params[paramRule.parameterName]
        
        // Check required parameters
        if (paramRule.required && paramValue == null) {
            return false
        }
        
        // Check forbidden parameters
        if (paramRule.forbidden && paramValue != null) {
            return false
        }
        
        // Check value pattern if specified
        if (paramRule.valuePattern != null && paramValue != null) {
            val valueRegex = Regex(paramRule.valuePattern)
            if (!valueRegex.matches(paramValue)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Initialize default security rules
     */
    private fun initializeDefaultRules() {
        // File system tools - generally safe
        addRule(PolicyRule(
            name = "allow_read_file",
            description = "Allow reading files",
            toolPattern = "read-file",
            decision = PolicyDecision.ALLOW,
            riskLevel = RiskLevel.LOW,
            priority = 10
        ))
        
        addRule(PolicyRule(
            name = "allow_write_file",
            description = "Allow writing files",
            toolPattern = "write-file",
            decision = PolicyDecision.ALLOW,
            riskLevel = RiskLevel.MEDIUM,
            priority = 10
        ))
        
        addRule(PolicyRule(
            name = "allow_glob",
            description = "Allow file globbing",
            toolPattern = "glob",
            decision = PolicyDecision.ALLOW,
            riskLevel = RiskLevel.LOW,
            priority = 10
        ))
        
        addRule(PolicyRule(
            name = "allow_grep",
            description = "Allow text searching",
            toolPattern = "grep",
            decision = PolicyDecision.ALLOW,
            riskLevel = RiskLevel.LOW,
            priority = 10
        ))
        
        // Shell commands - higher risk, but allowed for coding tasks
        addRule(PolicyRule(
            name = "allow_shell",
            description = "Allow shell commands",
            toolPattern = "shell",
            decision = PolicyDecision.ALLOW,
            riskLevel = RiskLevel.HIGH,
            priority = 5
        ))
    }
}
