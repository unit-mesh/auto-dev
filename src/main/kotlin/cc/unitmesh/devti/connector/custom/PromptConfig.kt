package cc.unitmesh.devti.connector.custom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class PromptItem(val instruction: String, val input: String)

@Serializable
data class PromptConfig(
    @SerialName("auto_complete")
    val autoComplete: PromptItem,
    @SerialName("auto_comment")
    val autoComment: PromptItem,
    @SerialName("code_review")
    val codeReview: PromptItem,
    @SerialName("find_bug")
    val findBug: PromptItem
) {
    companion object {
        fun fromString(json: String): PromptConfig {
            return Json { ignoreUnknownKeys = true }.decodeFromString(json)
        }
    }
}