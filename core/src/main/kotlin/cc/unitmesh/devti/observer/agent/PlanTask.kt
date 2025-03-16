package cc.unitmesh.devti.observer.agent

import kotlinx.serialization.Serializable

/**
 * 计划任务，描述了一个具体任务的细节和状态
 * @property description 任务描述
 * @property completed 任务是否已完成
 * @property status 任务状态（COMPLETED, FAILED, IN_PROGRESS, TODO）
 */
@Serializable
class PlanTask(
    val description: String,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO
) {
    companion object {
        private val COMPLETED_PATTERN = Regex("^\\[(✓|x|X)\\]\\s*(.*)")
        private val FAILED_PATTERN = Regex("^\\[!\\]\\s*(.*)")
        private val IN_PROGRESS_PATTERN = Regex("^\\[\\*\\]\\s*(.*)")
        private val TODO_PATTERN = Regex("^\\[\\s*\\]\\s*(.*)")

        /**
         * 从文本创建计划任务
         * @param text 任务文本，可能包含状态标记如 [✓]、[!]、[*] 或 [ ]
         * @return 创建的计划任务对象
         */
        fun fromText(text: String): PlanTask {
            return when {
                COMPLETED_PATTERN.matches(text) -> {
                    val description = COMPLETED_PATTERN.find(text)?.groupValues?.get(2) ?: text
                    PlanTask(description, true, TaskStatus.COMPLETED)
                }
                FAILED_PATTERN.matches(text) -> {
                    val description = FAILED_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    PlanTask(description, false, TaskStatus.FAILED)
                }
                IN_PROGRESS_PATTERN.matches(text) -> {
                    val description = IN_PROGRESS_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    PlanTask(description, false, TaskStatus.IN_PROGRESS)
                }
                TODO_PATTERN.matches(text) -> {
                    val description = TODO_PATTERN.find(text)?.groupValues?.get(1) ?: text
                    PlanTask(description, false, TaskStatus.TODO)
                }
                else -> PlanTask(text)
            }
        }
    }
    
    /**
     * 将任务转换为标准格式的文本表示
     * @return 包含状态标记的文本，如 [✓] Task description
     */
    fun toText(): String {
        val statusMarker = when (status) {
            TaskStatus.COMPLETED -> "[✓]"
            TaskStatus.FAILED -> "[!]"
            TaskStatus.IN_PROGRESS -> "[*]"
            TaskStatus.TODO -> "[ ]"
        }
        return "$statusMarker $description"
    }
    
    /**
     * 更新任务状态
     * @param newStatus 新的任务状态
     */
    fun updateStatus(newStatus: TaskStatus) {
        status = newStatus
        completed = (status == TaskStatus.COMPLETED)
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