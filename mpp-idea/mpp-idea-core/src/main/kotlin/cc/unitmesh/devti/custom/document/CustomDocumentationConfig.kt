package cc.unitmesh.devti.custom.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Example:
 * {
 *   "title": "Swagger annotation",
 *   "start": "/**",
 *   "end": "*/",
 *   "type": "normal",
 *   "example": {
 *     "question": "What is the meaning of life?",
 *     "answer": "42"
 *   }
 * }
 */
@Serializable
class CustomDocumentationConfig(
    val title: String,
    val prompt: String,
    val start: String,
    val end: String,
    val type: LivingDocumentationType,
    // for annotated example: @ApiParam(name = "id", value = "id of the user")
    val example: CustomDocumentationExample?,
) {

    companion object {
        // should only use for default intention, not for custom intention
        fun default(): CustomDocumentationConfig = CustomDocumentationConfig(
            "Nothing",
            "prompt",
            "/**",
            "*/",
            LivingDocumentationType.COMMENT,
            null
        )
    }
}

@Serializable
class CustomDocumentationExample(
    val question: String,
    val answer: String,
)

@Serializable
enum class LivingDocumentationType {
    @SerialName("normal")
    COMMENT,

    @SerialName("annotated")
    ANNOTATED,

    @SerialName("custom")
    CUSTOM
}