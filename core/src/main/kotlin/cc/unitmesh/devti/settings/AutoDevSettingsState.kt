package cc.unitmesh.devti.settings

import cc.unitmesh.devti.settings.locale.DEFAULT_HUMAN_LANGUAGE
import cc.unitmesh.devti.settings.locale.HUMAN_LANGUAGES
import cc.unitmesh.devti.settings.miscs.MAX_TOKEN_LENGTH
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "cc.unitmesh.devti.settings.DevtiSettingsState", storages = [Storage("DevtiSettings.xml")])
class AutoDevSettingsState : PersistentStateComponent<AutoDevSettingsState> {
    var delaySeconds = ""

    // Legacy fields - kept for backward compatibility but deprecated
    @Deprecated("Use defaultModelId instead")
    var customOpenAiHost = ""
    @Deprecated("Use LlmConfig system instead")
    var customEngineServer = ""
    @Deprecated("Use LlmConfig system instead")
    var customEngineToken = ""
    @Deprecated("Use defaultModelId instead")
    var customModel = ""

    // Main LLM configuration storage
    var customLlms = ""

    // Default model configuration
    var defaultModelId = "" // The default model to use when no specific model is set for a category
    var useDefaultForAllCategories = true // Whether to use default model for all categories

    // Selected model IDs for different categories (only used when useDefaultForAllCategories = false)
    var selectedPlanModelId = ""
    var selectedActModelId = ""
    var selectedCompletionModelId = ""
    var selectedEmbeddingModelId = ""
    var selectedFastApplyModelId = ""

    // Legacy fields - kept for backward compatibility but deprecated
    @Deprecated("Use LlmConfig system instead")
    var customEngineResponseFormat = "\$.choices[0].delta.content"
    @Deprecated("Use LlmConfig system instead")
    var customEngineRequestFormat = """{ "customFields": {"model": "deepseek-chat", "temperature": 0.0, "stream": true} }"""

    var language = DEFAULT_HUMAN_LANGUAGE
    var maxTokenLength = MAX_TOKEN_LENGTH.toString()

    fun fetchMaxTokenLength(): Int = maxTokenLength.toIntOrNull() ?: MAX_TOKEN_LENGTH

    fun fetchLocalLanguage(display: String = language) : String {
        return HUMAN_LANGUAGES.getAbbrByDispay(display)
    }

    @Synchronized
    override fun getState(): AutoDevSettingsState = this

    @Synchronized
    override fun loadState(state: AutoDevSettingsState) = XmlSerializerUtil.copyBean(state, this)

    companion object {
        val maxTokenLength: Int get() = getInstance().fetchMaxTokenLength()
        val language: String get() = getInstance().fetchLocalLanguage()

        fun getInstance(): AutoDevSettingsState {
            val application = ApplicationManager.getApplication()
            return if (application != null) {
                application.getService(AutoDevSettingsState::class.java).state
            } else {
                // Return a default instance for testing environments where ApplicationManager is not available
                AutoDevSettingsState()
            }
        }
    }
}
