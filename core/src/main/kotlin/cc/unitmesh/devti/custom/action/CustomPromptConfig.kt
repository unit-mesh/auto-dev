package cc.unitmesh.devti.custom.action

import cc.unitmesh.devti.custom.document.CustomDocumentationConfig
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CustomPromptConfig(
    @SerialName("spec")
    val spec: Map<String, String> = mapOf(),
    @SerialName("prompts")
    val prompts: List<CustomIntentionConfig> = listOf(),
    @SerialName("documentations")
    val documentations: List<CustomDocumentationConfig>? = null
) {
    companion object {
        private val logger = logger<CustomPromptConfig>()

        fun load(): CustomPromptConfig {
            return tryParse(AutoDevSettingsState.getInstance().customPrompts)
        }

        fun default(): CustomPromptConfig = CustomPromptConfig(
            mapOf(
                "controller" to "",
                "service" to "",
                "entity" to "",
                "repository" to ""
            )
        )

        fun tryParse(prompts: String?): CustomPromptConfig {
            if (prompts.isNullOrEmpty() || prompts == "\"\"") {
                return default()
            }

            try {
                return Json.decodeFromString(prompts)
            } catch (e: Exception) {
                logger.warn("Not found custom prompt, will use default: $e")
            }

            return default()
        }
    }
}