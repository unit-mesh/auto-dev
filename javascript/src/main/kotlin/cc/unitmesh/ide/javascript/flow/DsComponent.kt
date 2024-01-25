package cc.unitmesh.ide.javascript.flow

import kotlinx.serialization.Serializable

/**
 * the Design System Component
 */
@Serializable
data class DsComponent(
    val name: String,
    val props: List<String>,
    val events: List<String>
)