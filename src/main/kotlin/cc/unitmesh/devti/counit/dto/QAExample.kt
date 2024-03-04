package cc.unitmesh.devti.counit.dto

import kotlinx.serialization.Serializable

@Serializable
data class QAExample(
    val question: String,
    val answer: String,
)