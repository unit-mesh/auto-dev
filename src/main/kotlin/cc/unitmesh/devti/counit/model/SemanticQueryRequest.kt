package cc.unitmesh.devti.counit.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class SemanticQueryRequest(
    val q: String,
    @SerializedName("repo_ref")
    val repoRef: String,
    @SerializedName("type")
    val payloadType: PayloadType,
)

enum class PayloadType {
    Code,
    Comment,
    Doc,
    HttpApi,
    OpenApi,
    DatabaseMap
}