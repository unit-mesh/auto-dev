package cc.unitmesh.devti.observer.plan

import kotlinx.serialization.Serializable

/**
 * 计划任务，描述了一个具体任务的细节和状态
 * @property step 任务描述
 * @property completed 任务是否已完成
 * @property status 任务状态（COMPLETED, FAILED, IN_PROGRESS, TODO）
 * @property subSteps 子任务列表，用于支持嵌套任务结构
 * @property codeFileLinks 代码文件链接列表
 */
@Serializable
class AgentPlanStep(
    val step: String,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO,
    var subSteps: MutableList<AgentPlanStep> = mutableListOf(),
    var codeFileLinks: List<CodeFileLink> = emptyList()
) {
    companion object {
        private val COMPLETED_PATTERN = Regex("^\\[(✓|x|X)\\]\\s*(.*)")
        private val FAILED_PATTERN = Regex("^\\[!\\]\\s*(.*)")
        private val IN_PROGRESS_PATTERN = Regex("^\\[\\*\\]\\s*(.*)")
        private val TODO_PATTERN = Regex("^\\[\\s*\\]\\s*(.*)")

        fun fromText(text: String): AgentPlanStep {
            return when {
                COMPLETED_PATTERN.matches(text) -> {
                    val description = COMPLETED_PATTERN.find(text)?.groupValues?.get(2) ?: text
                    AgentPlanStep(description, true, TaskStatus.COMPLETED)
                }
                FAILED_PATTERN.matches(text) -> {
                    val description = FAILED_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    AgentPlanStep(description, false, TaskStatus.FAILED)
                }
                IN_PROGRESS_PATTERN.matches(text) -> {
                    val description = IN_PROGRESS_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    AgentPlanStep(description, false, TaskStatus.IN_PROGRESS)
                }
                TODO_PATTERN.matches(text) -> {
                    val description = TODO_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    AgentPlanStep(description, false, TaskStatus.TODO)
                }
                else -> AgentPlanStep(text)
            }
        }
    }

    fun toText(): String {
        val statusMarker = when (status) {
            TaskStatus.COMPLETED -> "[✓]"
            TaskStatus.FAILED -> "[!]"
            TaskStatus.IN_PROGRESS -> "[*]"
            TaskStatus.TODO -> "[ ]"
        }
        return "$statusMarker $step"
    }

    fun updateStatus(newStatus: TaskStatus, updateSubtasks: Boolean = false) {
        status = newStatus
        completed = (status == TaskStatus.COMPLETED)
        
        if (updateSubtasks && subSteps.isNotEmpty()) {
            subSteps.forEach { it.updateStatus(newStatus, true) }
        }
    }

    fun addSubtask(subtask: AgentPlanStep) {
        subSteps.add(subtask)
    }

    fun updateStatusFromSubtasks() {
        if (subSteps.isEmpty()) {
            return
        }

        when {
            subSteps.all { it.status == TaskStatus.COMPLETED } -> {
                status = TaskStatus.COMPLETED
                completed = true
            }
            subSteps.any { it.status == TaskStatus.FAILED } -> {
                status = TaskStatus.FAILED
                completed = false
            }
            subSteps.any { it.status == TaskStatus.IN_PROGRESS } -> {
                status = TaskStatus.IN_PROGRESS
                completed = false
            }
            else -> {
                status = TaskStatus.TODO
                completed = false
            }
        }
    }
}

/**
 * 任务状态枚举
 */
@Serializable
enum class TaskStatus {
    COMPLETED,
    FAILED,
    IN_PROGRESS,
    TODO
}