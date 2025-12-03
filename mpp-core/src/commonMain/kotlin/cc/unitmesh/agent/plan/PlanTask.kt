package cc.unitmesh.agent.plan

import cc.unitmesh.agent.plan.TaskStatus.Companion.toMarker
import kotlinx.serialization.Serializable

/**
 * Represents a task in a plan, containing multiple steps.
 * 
 * A task is a logical grouping of related steps that together
 * accomplish a specific goal.
 * 
 * Markdown format:
 * ```
 * 1. Task Title
 *    - [✓] Step 1
 *    - [*] Step 2
 *    - [ ] Step 3
 * ```
 */
@Serializable
data class PlanTask(
    /**
     * Unique identifier for this task
     */
    val id: String,
    
    /**
     * Title/name of this task
     */
    val title: String,
    
    /**
     * Steps within this task
     */
    val steps: MutableList<PlanStep> = mutableListOf(),
    
    /**
     * Current status of this task (derived from steps or set manually)
     */
    var status: TaskStatus = TaskStatus.TODO,
    
    /**
     * Current phase of this task (PDCA cycle)
     */
    var phase: PlanPhase = PlanPhase.PLAN
) {
    /**
     * Whether all steps in this task are completed
     */
    val isCompleted: Boolean
        get() = steps.isNotEmpty() && steps.all { it.isCompleted }
    
    /**
     * Progress percentage (0-100)
     */
    val progressPercent: Int
        get() = if (steps.isEmpty()) 0 
                else (steps.count { it.isCompleted } * 100) / steps.size
    
    /**
     * Number of completed steps
     */
    val completedStepCount: Int
        get() = steps.count { it.isCompleted }
    
    /**
     * Total number of steps
     */
    val totalStepCount: Int
        get() = steps.size
    
    /**
     * Add a step to this task
     */
    fun addStep(step: PlanStep) {
        steps.add(step)
        updateStatusFromSteps()
    }
    
    /**
     * Update a step's status by step ID
     */
    fun updateStepStatus(stepId: String, newStatus: TaskStatus) {
        steps.find { it.id == stepId }?.updateStatus(newStatus)
        updateStatusFromSteps()
    }
    
    /**
     * Complete a step by ID
     */
    fun completeStep(stepId: String) {
        updateStepStatus(stepId, TaskStatus.COMPLETED)
    }
    
    /**
     * Update task status based on step statuses
     */
    fun updateStatusFromSteps() {
        if (steps.isEmpty()) return
        
        status = when {
            steps.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
            steps.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
            steps.any { it.status == TaskStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
            steps.any { it.status == TaskStatus.BLOCKED } -> TaskStatus.BLOCKED
            else -> TaskStatus.TODO
        }
    }
    
    /**
     * Manually update task status (also updates all steps if completing)
     */
    fun updateStatus(newStatus: TaskStatus, updateSteps: Boolean = false) {
        status = newStatus
        if (updateSteps && newStatus == TaskStatus.COMPLETED) {
            steps.forEach { it.complete() }
        }
    }
    
    /**
     * Convert to markdown format
     */
    fun toMarkdown(index: Int): String {
        val sb = StringBuilder()
        sb.appendLine("$index. $title")
        steps.forEach { step ->
            sb.appendLine("   ${step.toMarkdown()}")
        }
        return sb.toString()
    }
    
    companion object {
        private val TASK_HEADER_PATTERN = Regex("^(\\d+)\\.\\s*(?:\\[([xX!*✓]?)\\]\\s*)?(.+?)(?:\\s*\\[([xX!*✓]?)\\])?$")
        
        /**
         * Parse task header from markdown
         */
        fun parseHeader(text: String): Triple<Int, String, TaskStatus>? {
            val match = TASK_HEADER_PATTERN.find(text.trim()) ?: return null
            val index = match.groupValues[1].toIntOrNull() ?: return null
            val title = match.groupValues[3].trim()
            val startMarker = match.groupValues[2]
            val endMarker = match.groupValues[4]
            val marker = startMarker.ifEmpty { endMarker }
            
            return Triple(index, title, TaskStatus.fromMarker(marker))
        }
        
        private var idCounter = 0L
        
        fun generateId(): String {
            return "task_${++idCounter}_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}"
        }
    }
}

