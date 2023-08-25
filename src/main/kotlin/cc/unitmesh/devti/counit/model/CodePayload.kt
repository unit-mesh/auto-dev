package cc.unitmesh.devti.counit.model

import cc.unitmesh.devti.counit.dto.PayloadType
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

typealias Embedding = List<Float>

@Serializable
data class CodePayload(
    val lang: String,
    @SerializedName("repo_name") val repoName: String,
    @SerializedName("repo_ref") val repoRef: String,
    @SerializedName("payload_type") val payloadType: PayloadType,
    @SerializedName("relative_path") val relativePath: String,
    @SerializedName("content_hash") val contentHash: String,
    @SerializedName("display_text") val displayText: String,
    @SerializedName("origin_text") val originText: String,
    @SerializedName("branches") val branches: List<String>,
    val id: String?,
    val embedding: Embedding?,
    val score: Float?,
)
