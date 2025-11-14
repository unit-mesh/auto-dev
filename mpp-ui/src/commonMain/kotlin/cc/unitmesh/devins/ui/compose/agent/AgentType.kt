package cc.unitmesh.devins.ui.compose.agent

/**
 * Unified Agent Type Enum
 *
 * Represents all available agent modes in the system:
 * - LOCAL: Simple local chat mode without heavy tooling
 * - CODING: Local coding agent with full tool access (file system, shell, etc.)
 * - CODE_REVIEW: Dedicated code review agent with git integration
 * - REMOTE: Remote agent connected to mpp-server
 */
enum class AgentType {
    /**
     * Local chat mode - lightweight conversation without heavy tooling
     */
    LOCAL,

    /**
     * Coding agent mode - full-featured local agent with file system, shell, and all tools
     */
    CODING,

    /**
     * Code review mode - specialized agent for code analysis and review
     */
    CODE_REVIEW,

    /**
     * Remote agent mode - connects to remote mpp-server for distributed execution
     */
    REMOTE;

    fun getDisplayName(): String = when (this) {
        LOCAL -> "Local Chat"
        CODING -> "Coding Agent"
        CODE_REVIEW -> "Code Review"
        REMOTE -> "Remote Agent"
    }

    /**
     * Check if this agent type requires remote connection
     */
    fun isRemote(): Boolean = this == REMOTE

    /**
     * Check if this agent type supports local file operations
     */
    fun supportsLocalFileOps(): Boolean = this in listOf(CODING, CODE_REVIEW)
}
