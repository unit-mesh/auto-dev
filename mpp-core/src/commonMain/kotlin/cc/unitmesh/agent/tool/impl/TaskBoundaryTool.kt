package cc.unitmesh.agent.tool.impl

import cc.unitmesh.agent.tool.*
import cc.unitmesh.agent.tool.schema.DeclarativeToolSchema
import cc.unitmesh.agent.tool.schema.SchemaPropertyBuilder.string
import cc.unitmesh.agent.tool.schema.ToolCategory
import kotlinx.serialization.Serializable

/**
 * Task status enum matching Cursor's task boundary behavior
 */
enum class TaskStatus {
    PLANNING,
    WORKING,
    COMPLETED,
    BLOCKED,
    CANCELLED
}

/**
 * Parameters for task boundary tool
 */
@Serializable
data class TaskBoundaryParams(
    /**
     * The name/title of the task - used as the header in UI
     * Keep the same taskName to update an existing task, change it to create a new task block
     */
    val taskName: String,
    
    /**
     * Current status of the task (PLANNING, WORKING, COMPLETED, BLOCKED, CANCELLED)
     */
    val status: String,
    
    /**
     * Brief summary describing what this task accomplishes or what you're doing
     */
    val summary: String = ""
)

/**
 * Schema for task boundary tool
 */
object TaskBoundarySchema : DeclarativeToolSchema(
    description = "Communicate task progress through a structured UI. Use this to keep users informed of your work.",
    properties = mapOf(
        "taskName" to string(
            description = "Task name/title - used as the header. Keep the same name to update an existing task, change it to create a new task block",
            required = true,
            maxLength = 100
        ),
        "status" to string(
            description = "Current task status",
            required = true,
            enum = listOf("PLANNING", "WORKING", "COMPLETED", "BLOCKED", "CANCELLED")
        ),
        "summary" to string(
            description = "Brief summary of what this task does or current activity",
            required = false,
            maxLength = 500
        )
    )
) {
    override fun getExampleUsage(toolName: String): String {
        return """/$toolName taskName="Planning Authentication" status="PLANNING" summary="Analyzing existing auth structure and planning OAuth2 implementation""""
    }
}

/**
 * Tool invocation for task boundary
 */
class TaskBoundaryInvocation(
    params: TaskBoundaryParams,
    tool: TaskBoundaryTool
) : BaseToolInvocation<TaskBoundaryParams, ToolResult>(params, tool) {
    
    override fun getDescription(): String {
        return "Task: ${params.taskName} [${params.status}]"
    }
    
    override fun getToolLocations(): List<ToolLocation> = emptyList()
    
    override suspend fun execute(context: ToolExecutionContext): ToolResult {
        // Validate status
        val status = try {
            TaskStatus.valueOf(params.status.uppercase())
        } catch (e: IllegalArgumentException) {
            return ToolResult.Error(
                message = "Invalid status: ${params.status}. Must be one of: ${TaskStatus.values().joinToString(", ")}",
                errorType = ToolErrorType.PARAMETER_OUT_OF_RANGE.code
            )
        }
        
        // Create metadata for tracking
        val metadata = mapOf(
            "task_name" to params.taskName,
            "status" to status.name,
            "summary" to params.summary
        )
        
        // Format the output message
        val output = buildString {
            appendLine("📋 Task Update")
            appendLine("Name: ${params.taskName}")
            appendLine("Status: ${status.name}")
            if (params.summary.isNotEmpty()) {
                appendLine("Summary: ${params.summary}")
            }
        }
        
        return ToolResult.Success(output, metadata)
    }
}

/**
 * Task Boundary Tool - inspired by Cursor's task management
 * 
 * ## Purpose
 * Communicate progress through a structured task UI. This helps users understand what you're working on
 * and track your progress through complex multi-step tasks.
 * 
 * ## UI Behavior
 * - taskName = Header of the UI block
 * - summary = Description of this task
 * - status = Current activity (PLANNING, WORKING, COMPLETED, BLOCKED, CANCELLED)
 * 
 * ## Usage Pattern
 * 
 * **First call**: Set taskName using the mode and work area (e.g., "Planning Authentication"), 
 * set summary to briefly describe the goal, set status to what you're about to start doing.
 * 
 * **Updates**: 
 * - Same taskName + updated summary/status = Updates accumulate in the same UI block
 * - Different taskName = Starts a new UI block with a fresh summary for the new task
 * 
 * ## When to Use
 * - For complex tasks with multiple steps (3+ steps)
 * - When you want to communicate progress during long-running operations
 * - To signal major phase transitions (planning -> implementation -> testing)
 * 
 * ## When NOT to Use
 * - Simple one-step tasks (answering questions, quick refactors)
 * - Single-file edits that don't affect many lines
 * - Trivial operations
 * 
 * ## Example Flow
 * 
 * ```
 * /task-boundary taskName="Implementing User Authentication" status="PLANNING" summary="Analyzing existing code structure"
 * // ... do some analysis ...
 * /task-boundary taskName="Implementing User Authentication" status="WORKING" summary="Adding JWT token validation"
 * // ... make changes ...
 * /task-boundary taskName="Implementing User Authentication" status="COMPLETED" summary="Authentication implemented and tested"
 * ```
 */
class TaskBoundaryTool : BaseExecutableTool<TaskBoundaryParams, ToolResult>() {
    
    override val name: String = "task-boundary"
    override val description: String = """
        Communicate task progress through a structured UI. Use this for complex multi-step tasks to keep users informed.
        
        - First call: Set taskName, initial status (usually PLANNING), and summary describing the goal
        - Updates: Use same taskName to update an existing task, or change taskName to create a new task block
        - Status options: PLANNING, WORKING, COMPLETED, BLOCKED, CANCELLED
        
        Skip for simple tasks (quick refactors, answering questions, single-file edits).
    """.trimIndent()
    
    override val metadata: ToolMetadata = ToolMetadata(
        displayName = "Task Boundary",
        tuiEmoji = "📋",
        composeIcon = "task",
        category = ToolCategory.Utility,
        schema = TaskBoundarySchema
    )
    
    override fun getParameterClass(): String = TaskBoundaryParams::class.simpleName ?: "TaskBoundaryParams"
    
    override fun createToolInvocation(params: TaskBoundaryParams): ToolInvocation<TaskBoundaryParams, ToolResult> {
        // Validate parameters
        validateParameters(params)
        return TaskBoundaryInvocation(params, this)
    }
    
    private fun validateParameters(params: TaskBoundaryParams) {
        if (params.taskName.isBlank()) {
            throw ToolException("Task name cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        
        if (params.status.isBlank()) {
            throw ToolException("Status cannot be empty", ToolErrorType.MISSING_REQUIRED_PARAMETER)
        }
        
        // Validate status is a valid enum value
        try {
            TaskStatus.valueOf(params.status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ToolException(
                "Invalid status: ${params.status}. Must be one of: ${TaskStatus.values().joinToString(", ")}",
                ToolErrorType.PARAMETER_OUT_OF_RANGE
            )
        }
    }
}

