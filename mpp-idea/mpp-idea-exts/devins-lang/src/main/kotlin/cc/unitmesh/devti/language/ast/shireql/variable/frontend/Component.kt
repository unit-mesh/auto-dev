package cc.unitmesh.devti.language.ast.shireql.variable.frontend

import kotlinx.serialization.Serializable

/**
 * the Design System Component
 */
@Serializable
data class Component(
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