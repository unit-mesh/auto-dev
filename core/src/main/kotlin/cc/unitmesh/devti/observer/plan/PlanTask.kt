package cc.unitmesh.devti.observer.plan

import kotlinx.serialization.Serializable

/**
 * 计划任务，描述了一个具体任务的细节和状态
 * @property step 任务描述
 * @property completed 任务是否已完成
 * @property status 任务状态（COMPLETED, FAILED, IN_PROGRESS, TODO）
 * @property subtasks 子任务列表，用于支持嵌套任务结构
 */
@Serializable
class PlanTask(
    val step: String,
    var completed: Boolean = false,
    var status: TaskStatus = TaskStatus.TODO,
    var subtasks: MutableList<PlanTask> = mutableListOf()
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
        return "$statusMarker $step"
    }
    
    /**
     * 更新任务状态
     * @param newStatus 新的任务状态
     * @param updateSubtasks 是否同时更新所有子任务的状态
     */
    fun updateStatus(newStatus: TaskStatus, updateSubtasks: Boolean = false) {
        status = newStatus
        completed = (status == TaskStatus.COMPLETED)
        
        if (updateSubtasks && subtasks.isNotEmpty()) {
            subtasks.forEach { it.updateStatus(newStatus, true) }
        }
    }
    
    /**
     * 添加子任务
     * @param subtask 要添加的子任务
     */
    fun addSubtask(subtask: PlanTask) {
        subtasks.add(subtask)
    }
    
    /**
     * 根据子任务状态更新当前任务状态
     * 如果所有子任务都已完成，则当前任务也标记为完成
     */
    fun updateStatusFromSubtasks() {
        if (subtasks.isEmpty()) {
            return
        }
        
        // 如果所有子任务都完成，则当前任务也完成
        if (subtasks.all { it.status == TaskStatus.COMPLETED }) {
            status = TaskStatus.COMPLETED
            completed = true
        }
        // 如果有任何子任务失败，则当前任务也标记为失败
        else if (subtasks.any { it.status == TaskStatus.FAILED }) {
            status = TaskStatus.FAILED
            completed = false
        }
        // 如果有任何子任务进行中，则当前任务也标记为进行中
        else if (subtasks.any { it.status == TaskStatus.IN_PROGRESS }) {
            status = TaskStatus.IN_PROGRESS
            completed = false
        }
        // 否则任务仍然是 TODO 状态
        else {
            status = TaskStatus.TODO
            completed = false
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