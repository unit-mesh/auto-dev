package cc.unitmesh.devti.custom

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Example:
 * {
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
    val start: String,
    val end: String,
    val type: LivingDocumentationType,
    // for annotated example: @ApiParam(name = "id", value = "id of the user")
    val example: CustomDocumentationExample?,
) {

}

@Serializable
class CustomDocumentationExample(
    val question: String,
    val answer: String
) {

}

@Serializable
enum class LivingDocumentationType {
    @SerialName("normal")
    NORMAL,
    @SerialName("annotated")
    ANNOTATED,
    @SerialName("custom")
    CUSTOM
}