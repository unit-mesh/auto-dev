package cc.unitmesh.devti.observer.plan

import kotlinx.serialization.Serializable

@Serializable
enum class PlanPhase {
    PLAN,   // 计划阶段
    DO,     // 执行阶段
    CHECK,  // 检查阶段
    ACT     // 行动阶段
}

@Serializable
data class AgentTaskEntry(
    val title: String,
    val steps: List<AgentPlanStep>,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO,
    var phase: PlanPhase = PlanPhase.PLAN
) {
    fun updateCompletionStatus() {
        if (steps.isEmpty()) return

        if (this.status == TaskStatus.COMPLETED) {
            completed = true
            steps.forEach { it.completed = true }
            return
        }

        completed = steps.all { it.status == TaskStatus.COMPLETED }

        status = when {
            steps.all { it.status == TaskStatus.COMPLETED } -> TaskStatus.COMPLETED
            steps.any { it.status == TaskStatus.FAILED } -> TaskStatus.FAILED
            steps.any { it.status == TaskStatus.IN_PROGRESS } -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.TODO
        }

        if (completed) {
            status = TaskStatus.COMPLETED
        }
    }

    fun updateStatus(status: TaskStatus) {
        this.status = status
        updateCompletionStatus()
    }
}
