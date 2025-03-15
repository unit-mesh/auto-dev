package cc.unitmesh.devti.observer.agent

data class PlanList(
    val title: String,
    val planTasks: List<PlanTask>,
    val completed: Boolean = false
)

/**
 * 表示计划项中的单个任务
 *
 * @property description 任务描述文本
 * @property completed 任务是否已完成
 */
data class PlanTask(
    val description: String,
    var completed: Boolean = false
) {
    companion object {
        /**
         * 从任务文本创建Task对象
         *
         * @param taskText 任务文本，可能包含完成标记
         * @return 封装了任务描述和状态的Task对象
         */
        fun fromText(taskText: String): PlanTask {
            val isCompleted = taskText.contains("✓") ||
                             Regex("\\[\\s*([xX])\\s*\\]").containsMatchIn(taskText)

            // 清理描述文本，移除完成标记
            val cleanedDescription = taskText
                .replace("✓", "")
                .replace(Regex("\\[\\s*[xX]\\s*\\]"), "[ ]")
                .replace(Regex("\\[\\s*\\]"), "")
                .trim()

            return PlanTask(cleanedDescription, isCompleted)
        }
    }
}