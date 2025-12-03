package cc.unitmesh.agent.plan

import kotlinx.serialization.Serializable

/**
 * Lightweight summary data for displaying plan status in UI.
 * 
 * This is a simplified view of AgentPlan optimized for UI display,
 * containing only the essential information needed for the summary bar.
 */
@Serializable
data class PlanSummaryData(
    val planId: String,
    val title: String,
    val totalSteps: Int,
    val completedSteps: Int,
    val failedSteps: Int,
    val progressPercent: Int,
    val status: TaskStatus,
    val currentStepDescription: String?,
    val tasks: List<TaskSummary>
) {
    companion object {
        /**
         * Create a PlanSummaryData from an AgentPlan
         */
        fun from(plan: AgentPlan): PlanSummaryData {
            val allSteps = plan.tasks.flatMap { it.steps }
            val completedSteps = allSteps.count { it.status == TaskStatus.COMPLETED }
            val failedSteps = allSteps.count { it.status == TaskStatus.FAILED }
            
            // Find current step (first in-progress or first todo)
            val currentStep = allSteps.firstOrNull { it.status == TaskStatus.IN_PROGRESS }
                ?: allSteps.firstOrNull { it.status == TaskStatus.TODO }
            
            // Title: use first task title or "Plan"
            val title = plan.tasks.firstOrNull()?.title ?: "Plan"
            
            return PlanSummaryData(
                planId = plan.id,
                title = title,
                totalSteps = allSteps.size,
                completedSteps = completedSteps,
                failedSteps = failedSteps,
                progressPercent = plan.progressPercent,
                status = plan.status,
                currentStepDescription = currentStep?.description,
                tasks = plan.tasks.map { TaskSummary.from(it) }
            )
        }
    }
}

/**
 * Summary of a single task within a plan
 */
@Serializable
data class TaskSummary(
    val id: String,
    val title: String,
    val status: TaskStatus,
    val completedSteps: Int,
    val totalSteps: Int,
    val steps: List<StepSummary>
) {
    companion object {
        fun from(task: PlanTask): TaskSummary {
            return TaskSummary(
                id = task.id,
                title = task.title,
                status = task.status,
                completedSteps = task.completedStepCount,
                totalSteps = task.totalStepCount,
                steps = task.steps.map { StepSummary.from(it) }
            )
        }
    }
}

/**
 * Summary of a single step within a task
 */
@Serializable
data class StepSummary(
    val id: String,
    val description: String,
    val status: TaskStatus
) {
    companion object {
        fun from(step: PlanStep): StepSummary {
            return StepSummary(
                id = step.id,
                description = step.description,
                status = step.status
            )
        }
    }
}
