package cc.unitmesh.devins.ui.compose.agent

/**
 * Enum representing available agent types
 */
enum class AgentType {
    LOCAL,
    CODING,
    CODE_REVIEW;

    fun getDisplayName(): String = when (this) {
        LOCAL -> "Local Agent"
        CODING -> "Coding Agent"
        CODE_REVIEW -> "Code Review"
    }
}
