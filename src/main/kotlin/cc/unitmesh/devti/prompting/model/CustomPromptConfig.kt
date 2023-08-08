package cc.unitmesh.devti.prompting.model

import cc.unitmesh.devti.custom.CustomIntentionPrompt
import cc.unitmesh.devti.gui.chat.ChatActionType.*
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CustomPromptConfig(
    @Deprecated("Use [cc.unitmesh.devti.custom.CustomPromptAction] instead")
    @SerialName("auto_complete")
    val autoComplete: CustomPromptItem,
    @Deprecated("Use [cc.unitmesh.devti.custom.CustomPromptAction] instead")
    @SerialName("auto_comment")
    val autoComment: CustomPromptItem,
    @Deprecated("Use [cc.unitmesh.devti.custom.CustomPromptAction] instead")
    @SerialName("code_review")
    val codeReview: CustomPromptItem,
    @Deprecated("Use [cc.unitmesh.devti.custom.CustomPromptAction] instead")
    @SerialName("refactor")
    val refactor: CustomPromptItem,
    @Deprecated("Use [cc.unitmesh.devti.custom.CustomPromptAction] instead")
    @SerialName("write_test")
    val writeTest: CustomPromptItem,
    @SerialName("spec")
    val spec: Map<String, String> = mapOf(),
    @SerialName("prompts")
    val prompts: List<CustomIntentionPrompt> = listOf(),
) {
    companion object {
        private val logger = logger<CustomPromptConfig>()

        fun load(): CustomPromptConfig {
            val config = tryParse(AutoDevSettingsState.getInstance().customEnginePrompts)
            logger.info("Loaded prompt config: $config")
            return config
        }

        fun default(): CustomPromptConfig = CustomPromptConfig(
            autoComplete = CustomPromptItem(CODE_COMPLETE.instruction(), "{code}"),
            autoComment = CustomPromptItem(EXPLAIN.instruction(), "{code}"),
            codeReview = CustomPromptItem(REVIEW.instruction(), "{code}"),
            refactor = CustomPromptItem(REFACTOR.instruction(), "{code}"),
            writeTest = CustomPromptItem(GENERATE_TEST.instruction(), "{code}"),

            mapOf(
                "controller" to "",
                "service" to "",
                "entity" to "",
                "repository" to "",
                "ddl" to ""
            )
        )

        fun tryParse(prompts: String?): CustomPromptConfig {
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