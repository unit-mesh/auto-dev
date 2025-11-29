package cc.unitmesh.devins.idea.model

/**
 * Agent types for the IntelliJ ToolWindow.
 * Mirrors the AgentType enum from mpp-core but adapted for IntelliJ plugin.
 */
enum class AgentType(val displayName: String) {
    /**
     * Full-featured coding agent with tools
     */
    CODING("Agentic"),

    /**
     * Code review mode - specialized agent for code analysis
     */
    CODE_REVIEW("Review"),

    /**
     * Document reader mode - AI-native document reading and analysis
     */
    KNOWLEDGE("Knowledge"),

    /**
     * Remote agent mode - connects to remote mpp-server
     */
    REMOTE("Remote");

    companion object {
        fun fromDisplayName(name: String): AgentType {
            return entries.find { it.displayName.equals(name, ignoreCase = true) }
                ?: CODING
        }
    }
}

