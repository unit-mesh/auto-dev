package cc.unitmesh.ide.javascript.flow.model

import kotlinx.serialization.Serializable

/**
 * the Design System Component
 */
@Serializable
data class DsComponent(
    val name: String,
    val path: String,
    val signature: String = "",
    val props: List<String> = emptyList(),
) {
    fun format(): String {
        return """
            |component name: $name
            |component path: $path
            |input signature: $signature
            |component props: $props
        """.trimMargin()
    }
}