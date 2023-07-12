package cc.unitmesh.devti.connector.custom

import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
) {
    companion object {
        private val logger = Logger.getInstance(PromptConfig::class.java)

        fun default(): PromptConfig {
            return PromptConfig(
                PromptItem("Auto complete code", "{code}"),
                PromptItem("Auto comment code", "{code}"),
                PromptItem("Code review code", "{code}"),
                PromptItem("Find bug", "{code}"),
                PromptItem("Write test code for", "{code}"),
            )
        }

        fun tryParse(prompts: String?): PromptConfig {
            if (prompts == null) {
                return default()
            }

            try {
                return Json.decodeFromString(prompts)
            } catch (e: Exception) {
                logger.error("Error parsing prompts: $e")
            }

            return default()
        }
    }
}