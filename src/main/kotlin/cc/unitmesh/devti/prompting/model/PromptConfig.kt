package cc.unitmesh.devti.prompting.model

import cc.unitmesh.devti.gui.chat.ChatActionType.*
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class PromptItem(val instruction: String, val input: String, val requirements: String = "")

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
    val writeTest: PromptItem,
    @SerialName("spec")
    val spec: Map<String, String> = mapOf()
) {
    companion object {
        private val logger = Logger.getInstance(PromptConfig::class.java)

        fun load(): PromptConfig {
            val config = tryParse(AutoDevSettingsState.getInstance().customEnginePrompts)
            logger.info("Loaded prompt config: $config")
            return config
        }

        fun default(): PromptConfig = PromptConfig(
            autoComplete = PromptItem(CODE_COMPLETE.instruction(), "{code}"),
            autoComment = PromptItem(EXPLAIN.instruction(), "{code}"),
            codeReview = PromptItem(REVIEW.instruction(), "{code}"),
            refactor = PromptItem(REFACTOR.instruction(), "{code}"),
            writeTest = PromptItem(WRITE_TEST.instruction(), "{code}"),
            mapOf(
                "controller" to "",
                "service" to "",
                "entity" to "",
                "repository" to "",
                "ddl" to ""
            )
        )

        fun tryParse(prompts: String?): PromptConfig {
            if (prompts.isNullOrEmpty() || prompts == "\"\"") {
                return default()
            }

            try {
                return Json.decodeFromString(prompts)
            } catch (e: Exception) {
                logger.info("Not found custom prompt, will use default: $e")
            }

            return default()
        }
    }
}