package cc.unitmesh.devti.bridge.archview.model

import kotlinx.serialization.Serializable

@Serializable
data class UiComponent(
    val name: String,
    val path: String,
    val signature: String = "",
    val props: List<String> = emptyList(),
    val methods: List<String> = emptyList(),
    val slots: List<String> = emptyList(),
) {
    fun format(): String {
        return """
            |<$name/>, path: $path
            |props: $props
            |methods: $methods
        """.trimMargin()
    }

    fun simple(): String = "<$name/>, $path"
}