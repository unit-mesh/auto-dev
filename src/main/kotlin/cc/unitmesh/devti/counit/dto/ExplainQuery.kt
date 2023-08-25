package cc.unitmesh.devti.counit.dto

import cc.unitmesh.devti.counit.model.CodePayload
import kotlinx.serialization.Serializable

@Serializable
data class ExplainQuery(
    val domain: String,
    val query: String,
    val natureLangQuery: String,
    val hypotheticalDocument: String,
)

class QueryResult(
    val normalQuery: List<CodePayload>,
    val natureLangQuery: List<CodePayload>,
    val hypotheticalDocument: List<CodePayload>,
)

@Serializable
data class QAExample(
    val question: String,
    val answer: ExplainQuery,
)