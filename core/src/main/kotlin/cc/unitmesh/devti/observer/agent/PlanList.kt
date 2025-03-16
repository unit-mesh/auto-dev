package cc.unitmesh.devti.observer.agent

import kotlinx.serialization.Serializable

@Serializable
data class PlanList(
    val title: String,
    val planTasks: List<PlanTask>,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO
) {
    /**
     * Updates the completion status based on the tasks' statuses
     */
    fun updateCompletionStatus() {
        if (planTasks.isEmpty()) return
        
        // Check if all tasks are completed
        completed = planTasks.all { it.status == TaskStatus.COMPLETED }
        
        // Determine section status based on tasks
        status = when {
            planTasks.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
            planTasks.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
            planTasks.any { it.status == TaskStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }
    }
}
