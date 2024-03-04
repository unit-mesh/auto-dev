package cc.unitmesh.devti.counit.model

import kotlinx.serialization.Serializable

typealias Embedding = List<Float>

@Serializable
data class CodePayload(
    val id: String?,
    val text: String?,
    val embedding: Embedding?,
    val score: Float?,
)
