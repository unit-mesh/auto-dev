package cc.unitmesh.agent.plan

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Represents a complete agent plan containing multiple tasks.
 * 
 * An AgentPlan is the top-level container for organizing work
 * into tasks and steps, with tracking for creation and update times.
 */
@Serializable
data class AgentPlan(
    /**
     * Unique identifier for this plan
     */
    val id: String,
    
    /**
     * Tasks in this plan
     */
    val tasks: MutableList<PlanTask> = mutableListOf(),
    
    /**
     * Timestamp when this plan was created (epoch milliseconds)
     */
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    
    /**
     * Timestamp when this plan was last updated (epoch milliseconds)
     */
    var updatedAt: Long = createdAt
) {
    /**
     * Overall status of the plan (derived from tasks)
     */
    val status: TaskStatus
        get() = when {
            tasks.isEmpty() -> TaskStatus.TODO
            tasks.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
            tasks.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
            tasks.any { it.status == TaskStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
            tasks.any { it.status == TaskStatus.BLOCKED } -> TaskStatus.BLOCKED
            else -> TaskStatus.TODO
        }
    
    /**
     * Overall progress percentage (0-100)
     */
    val progressPercent: Int
        get() {
            val totalSteps = tasks.sumOf { it.totalStepCount }
            if (totalSteps == 0) return 0
            val completedSteps = tasks.sumOf { it.completedStepCount }
            return (completedSteps * 100) / totalSteps
        }
    
    /**
     * Total number of tasks
     */
    val taskCount: Int
        get() = tasks.size
    
    /**
     * Number of completed tasks
     */
    val completedTaskCount: Int
        get() = tasks.count { it.isCompleted }
    
    /**
     * Add a task to this plan
     */
    fun addTask(task: PlanTask) {
        tasks.add(task)
        touch()
    }
    
    /**
     * Get a task by ID
     */
    fun getTask(taskId: String): PlanTask? {
        return tasks.find { it.id == taskId }
    }
    
    /**
     * Update a task's status
     */
    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        getTask(taskId)?.updateStatus(status)
        touch()
    }
    
    /**
     * Complete a step within a task
     */
    fun completeStep(taskId: String, stepId: String) {
        getTask(taskId)?.completeStep(stepId)
        touch()
    }
    
    /**
     * Update the updatedAt timestamp
     */
    private fun touch() {
        updatedAt = Clock.System.now().toEpochMilliseconds()
    }
    
    /**
     * Convert to markdown format
     */
    fun toMarkdown(): String {
        val sb = StringBuilder()
        tasks.forEachIndexed { index, task ->
            sb.append(task.toMarkdown(index + 1))
        }
        return sb.toString()
    }
    
    companion object {
        private var idCounter = 0L
        
        /**
         * Create a new plan with generated ID
         */
        fun create(tasks: List<PlanTask> = emptyList()): AgentPlan {
            return AgentPlan(
                id = generateId(),
                tasks = tasks.toMutableList()
            )
        }
        
        /**
         * Generate a unique plan ID
         */
        fun generateId(): String {
            return "plan_${++idCounter}_${Clock.System.now().toEpochMilliseconds()}"
        }
    }
}

