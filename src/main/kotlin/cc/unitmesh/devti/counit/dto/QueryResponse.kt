package cc.unitmesh.devti.counit.dto

import cc.unitmesh.devti.counit.model.CodePayload
import kotlinx.serialization.Serializable

@Serializable
data class QueryResponse(
    val data: List<CodePayload>
)