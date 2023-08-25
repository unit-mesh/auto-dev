package cc.unitmesh.devti.counit.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExplainQuery(
    val domain: String,
    val query: String,
    @SerialName("hypothetical_document")
    val hypotheticalDocument: String
)

@Serializable
data class QAExample(
    val question: String,
    val answer: ExplainQuery
)