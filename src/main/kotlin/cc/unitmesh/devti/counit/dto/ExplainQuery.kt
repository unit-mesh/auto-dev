package cc.unitmesh.devti.counit.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExplainQuery(
    val domain: String,
    val query: String,
    val hypotheticalDocument: String
)

@Serializable
data class QAExample(
    val question: String,
    val answer: ExplainQuery
)