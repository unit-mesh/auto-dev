package cc.unitmesh.devti.observer.agent

import kotlinx.serialization.Serializable

@Serializable
data class PlanList(
    val title: String,
    val planTasks: List<PlanTask>,
    val completed: Boolean = false
)

@Serializable
data class PlanTask(val description: String, var completed: Boolean = false) {
    companion object {
        fun fromText(taskText: String): PlanTask {
            val isCompleted = taskText.contains("✓") ||
                    Regex("\\[\\s*([xX])\\s*\\]").containsMatchIn(taskText)

            val cleanedDescription = taskText
                .replace("✓", "")
                .replace(Regex("\\[\\s*[xX]\\s*\\]"), "[ ]")
                .replace(Regex("\\[\\s*\\]"), "")
                .trim()

            return PlanTask(cleanedDescription, isCompleted)
        }
    }
}