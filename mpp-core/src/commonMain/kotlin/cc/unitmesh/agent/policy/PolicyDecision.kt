package cc.unitmesh.agent.policy

import kotlinx.serialization.Serializable

/**
 * Represents a policy decision for tool execution
 */
@Serializable
enum class PolicyDecision {
    /**
     * Allow the tool execution without any restrictions
     */
    ALLOW,
    
    /**
     * Deny the tool execution completely
     */
    DENY,
    
    /**
     * Allow the tool execution but require user confirmation
     */
    ASK_USER
}

/**
 * Detailed policy decision with reasoning
 */
@Serializable
data class PolicyDecisionResult(
    val decision: PolicyDecision,
    val reason: String,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Risk levels for tool execution
 */
@Serializable
enum class RiskLevel {
    LOW,      // Safe operations like reading files
    MEDIUM,   // Operations that modify files
    HIGH,     // Operations that execute commands or access network
    CRITICAL  // Operations that could damage the system
}

/**
 * Policy rule for tool execution
 */
@Serializable
data class PolicyRule(
    val name: String,
    val description: String,
    val toolPattern: String,  // Regex pattern for tool names
    val parameterRules: List<ParameterRule> = emptyList(),
    val decision: PolicyDecision,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val priority: Int = 0  // Higher priority rules are evaluated first
)

/**
 * Rule for specific tool parameters
 */
@Serializable
data class ParameterRule(
    val parameterName: String,
    val valuePattern: String?,  // Regex pattern for parameter values
    val required: Boolean = false,
    val forbidden: Boolean = false
)
