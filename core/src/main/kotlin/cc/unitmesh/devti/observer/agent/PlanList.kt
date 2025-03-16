package cc.unitmesh.devti.observer.agent

import kotlinx.serialization.Serializable

@Serializable
data class PlanList(
    val title: String,
    val planTasks: List<PlanTask>,
    val completed: Boolean = false
)
