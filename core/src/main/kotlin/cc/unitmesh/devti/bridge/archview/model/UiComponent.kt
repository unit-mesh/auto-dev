package cc.unitmesh.devti.bridge.archview.model

import kotlinx.serialization.Serializable

@Serializable
data class UiComponent(
    val name: String,
    val path: String,
    val signature: String = "",
    val props: List<String> = emptyList(),
) {
    fun format(): String {
        return """
            |path: $path
            |component: <$name >
            |props: $props
        """.trimMargin()
    }
}