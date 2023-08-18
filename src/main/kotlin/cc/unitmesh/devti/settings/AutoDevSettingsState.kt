package cc.unitmesh.devti.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "cc.unitmesh.devti.settings.DevtiSettingsState", storages = [Storage("DevtiSettings.xml")])
class AutoDevSettingsState : PersistentStateComponent<AutoDevSettingsState> {
    var githubToken = ""
    var openAiKey = ""
    var openAiModel = ""

    var aiEngine = DEFAULT_AI_ENGINE
    var customOpenAiHost = ""
    var customEngineServer = ""
    var customEngineToken = ""
    var customPrompts = ""

    /**
     * should be a json path
     */
    var customEngineResponseFormat = ""
    var language = DEFAULT_HUMAN_LANGUAGE
    var maxTokenLength = MAX_TOKEN_LENGTH.toString()

    fun fetchMaxTokenLength(): Int {
        return maxTokenLength.toIntOrNull() ?: MAX_TOKEN_LENGTH
    }

    @Synchronized
    override fun getState(): AutoDevSettingsState {
        return this
    }

    @Synchronized
    override fun loadState(state: AutoDevSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val maxTokenLength: Int get() = getInstance().fetchMaxTokenLength()

        fun getInstance(): AutoDevSettingsState {
            return ApplicationManager.getApplication().getService(AutoDevSettingsState::class.java).state
        }
    }

}
