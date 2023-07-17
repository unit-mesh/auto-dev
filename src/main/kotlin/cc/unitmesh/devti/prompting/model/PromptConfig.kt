package cc.unitmesh.devti.prompting.model

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

        /**
         * Load prompt config from settings
         */
        fun load(): PromptConfig {
            val config = tryParse(AutoDevSettingsState.getInstance().customEnginePrompts)
            logger.info("Loaded prompt config: $config")
            return config
        }

        fun default(): PromptConfig {
            val spec = mapOf(
                "controller" to "",
                "service" to "",
                "entity" to "",
                "repository" to "",
                "ddl" to ""
            )

            return PromptConfig(
                PromptItem("Complete java code, return rest code, no explaining.", "{code}"),
                PromptItem("Auto comment code", "{code}"),
                PromptItem("Code review code", "{code}"),
                PromptItem("Find bug", "{code}"),
                PromptItem("Write test code for", "{code}"),
                spec
            )
        }

        fun tryParse(prompts: String?): PromptConfig {
            if (prompts == null) {
                return default()
            }

            try {
                return Json.decodeFromString(prompts)
            } catch (e: Exception) {
                logger.warn("Error parsing prompts: $e")
            }

            return default()
        }
    }
}