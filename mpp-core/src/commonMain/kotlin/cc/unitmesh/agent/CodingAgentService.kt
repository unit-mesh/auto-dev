package cc.unitmesh.agent

/**
 * Result of an agent execution step
 */
data class AgentStep(
    val step: Int,
    val action: String,
    val tool: String? = null,
    val params: Any? = null,
    val result: String? = null,
    val success: Boolean
)

/**
 * Represents a file edit made by the agent
 */
data class AgentEdit(
    val file: String,
    val operation: AgentEditOperation,
    val content: String? = null
)

enum class AgentEditOperation {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Result of an agent task execution
 */
data class AgentResult(
    val success: Boolean,
    val message: String,
    val steps: List<AgentStep>,
    val edits: List<AgentEdit>
)

/**
 * Represents a task for the coding agent
 */
data class AgentTask(
    val requirement: String,
    val projectPath: String
)

/**
 * Coding Agent Service interface
 * 
 * This is the core abstraction for the autonomous coding agent.
 * Different platforms (JVM, JS, Android, iOS) can implement this interface
 * to provide platform-specific functionality while sharing the core logic.
 * 
 * The agent operates in a loop:
 * 1. Build context from project
 * 2. Generate system prompt using template
 * 3. Get next action from LLM
 * 4. Execute action using tools
 * 5. Repeat until task is complete or max iterations reached
 */
interface CodingAgentService {
    
    /**
     * Execute a development task
     * 
     * @param task The task to execute
     * @return The result of the task execution
     */
    suspend fun executeTask(task: AgentTask): AgentResult
    
    /**
     * Build system prompt for the agent
     * Uses CodingAgentContext and CodingAgentTemplate
     * 
     * @param context The context for prompt generation
     * @param language Language for the prompt (EN or ZH)
     * @return The rendered system prompt
     */
    fun buildSystemPrompt(context: CodingAgentContext, language: String = "EN"): String
    
    /**
     * Initialize workspace for the agent
     * This should scan the project structure, detect build tools, etc.
     * 
     * @param projectPath The path to the project
     */
    suspend fun initializeWorkspace(projectPath: String)
}


