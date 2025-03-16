package cc.unitmesh.devti.observer.plan

import kotlinx.serialization.Serializable

@Serializable
data class AgentPlan(
    val title: String,
    val tasks: List<PlanTask>,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO
) {
    /**
     * Updates the completion status based on the tasks' statuses
     */
    fun updateCompletionStatus() {
        if (tasks.isEmpty()) return
        
        // Check if all tasks are completed
        completed = tasks.all { it.status == TaskStatus.COMPLETED }
        
        // Determine section status based on tasks
        status = when {
            tasks.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
            tasks.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
            tasks.any { it.status == TaskStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }
    }
}
