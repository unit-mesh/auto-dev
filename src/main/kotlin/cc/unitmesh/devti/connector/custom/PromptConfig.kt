package cc.unitmesh.devti.connector.custom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("refactor")
    val refactor: PromptItem,
    @SerialName("write_test")
    val writeTest: PromptItem
)