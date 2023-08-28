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
    /**
     * the software source code was written in English, so query in English will be more accurate.
     */
    val englishQuery: List<CodePayload>,
    /**
     * the document, wiki, comments or OpenAPI's title was written by natural language, so query in natural
     * language will be more accurate.
     */
    val naturalLangQuery: List<CodePayload>,
    /**
     * with hypothetical document, we can get more accurate result in some cases.
     */
    val hypotheticalDocument: List<CodePayload>,
)

@Serializable
data class QAExample(
    val question: String,
    val answer: ExplainQuery,
)