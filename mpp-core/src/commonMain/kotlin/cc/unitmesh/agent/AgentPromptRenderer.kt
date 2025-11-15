package cc.unitmesh.agent

import cc.unitmesh.devins.compiler.variable.VariableTable

/**
 * Unified interface for agent prompt rendering
 * 
 * This interface provides a consistent approach for rendering system prompts
 * across different agent types (Coding, CodeReview, etc.)
 */
interface AgentPromptRenderer<T : AgentContext> {
    
    /**
     * Render a system prompt from context
     * 
     * @param context The agent context containing all necessary information
     * @param language Language for the prompt (EN or ZH)
     * @return The rendered system prompt
     */
    fun render(context: T, language: String = "EN"): String
}

/**
 * Base interface for agent contexts
 * 
 * All agent contexts should implement this interface to provide
 * consistent variable table conversion for template compilation
 */
interface AgentContext {
    
    /**
     * Convert context to variable table for template compilation
     * 
     * @return VariableTable containing all context variables
     */
    fun toVariableTable(): VariableTable
}

