package cc.unitmesh.devti.custom

import cc.unitmesh.devti.gui.chat.ChatActionType.*
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CustomPromptConfig(
    @SerialName("spec")
    val spec: Map<String, String> = mapOf(),
    @SerialName("prompts")
    val prompts: List<CustomIntentionConfig> = listOf(),
) {
    companion object {
        private val logger = logger<CustomPromptConfig>()

        fun load(): CustomPromptConfig {
            val config = tryParse(AutoDevSettingsState.getInstance().customEnginePrompts)
            logger.info("Loaded prompt config: $config")
            return config
        }

        fun default(): CustomPromptConfig = CustomPromptConfig(
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