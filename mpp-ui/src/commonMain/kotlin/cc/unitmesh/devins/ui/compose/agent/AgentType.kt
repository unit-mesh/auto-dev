package cc.unitmesh.devins.ui.compose.agent

/**
 * Enum representing available agent types
 */
enum class AgentType {
    CODING,
    CODE_REVIEW;

    fun getDisplayName(): String = when (this) {
        CODING -> "Coding Agent"
        CODE_REVIEW -> "Code Review"
    }
}
