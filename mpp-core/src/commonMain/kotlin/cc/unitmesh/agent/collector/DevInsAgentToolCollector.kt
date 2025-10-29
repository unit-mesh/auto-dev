package cc.unitmesh.agent.collector

import cc.unitmesh.agent.Tool

/**
 * Platform-independent interface for collecting agent tools.
 * This interface abstracts away platform-specific dependencies like IDEA Project.
 */
interface DevInsAgentToolCollector {
    /**
     * Collect available agent tools from the current context.
     * @param context Platform-specific context (e.g., IDEA Project, file system path, etc.)
     * @return List of available agent tools
     */
    fun collect(context: Any?): List<Tool>

    /**
     * Execute an agent tool with given parameters.
     * @param context Platform-specific context, for example Project in IDEA
     * @param agentName Name of the agent to execute
     * @param input Input parameters for the agent
     * @return Execution result or null if failed
     */
    suspend fun execute(context: Any?, agentName: String, input: String): String?
}
