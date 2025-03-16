package cc.unitmesh.devti.observer.plan

import kotlinx.serialization.Serializable

/**
 * 表示PDCA循环中的阶段
 */
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

    /**
     * 推进PDCA循环到下一阶段
     * @return 当前阶段
     */
    fun advancePdcaPhase(): PlanPhase {
        phase = when (phase) {
            PlanPhase.PLAN -> PlanPhase.DO
            PlanPhase.DO -> PlanPhase.CHECK
            PlanPhase.CHECK -> PlanPhase.ACT
            PlanPhase.ACT -> PlanPhase.PLAN // 完成一个循环后重新开始
        }
        return phase
    }

    /**
     * 设置PDCA循环的特定阶段
     * @param newPhase 要设置的新阶段
     */
    fun setPdcaPhase(newPhase: PlanPhase) {
        phase = newPhase
    }

    /**
     * 获取当前PDCA阶段
     * @return 当前PDCA阶段
     */
    fun getPdcaPhase(): PlanPhase = phase

    /**
     * 根据PDCA阶段更新计划和任务状态
     */
    fun processPdcaPhase() {
        when (phase) {
            PlanPhase.PLAN -> {
                // 计划阶段：任务准备就绪但尚未开始
                steps.forEach {
                    if (it.status == TaskStatus.TODO) {
                        // 保持任务为TODO状态
                    }
                }
            }

            PlanPhase.DO -> {
                // 执行阶段：将任务状态更新为进行中
                steps.forEach {
                    if (it.status == TaskStatus.TODO) {
                        it.updateStatus(TaskStatus.IN_PROGRESS)
                    }
                }
            }

            PlanPhase.CHECK -> {
                // 检查阶段：评估任务执行情况
                updateCompletionStatus()
            }

            PlanPhase.ACT -> {
                // 行动阶段：基于检查结果采取行动
                // 失败的任务可以在这里重置为TODO以便重试
                steps.forEach {
                    if (it.status == TaskStatus.FAILED) {
                        it.updateStatus(TaskStatus.TODO)
                    }
                }
            }
        }
    }
}
