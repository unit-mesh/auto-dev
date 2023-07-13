package cc.unitmesh.devti.settings

import cc.unitmesh.devti.prompting.PromptConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@State(name = "cc.unitmesh.devti.settings.DevtiSettingsState", storages = [Storage("DevtiSettings.xml")])
class AutoDevSettingsState : PersistentStateComponent<AutoDevSettingsState> {
    var githubToken = ""
    var openAiKey = ""
    var openAiModel = ""

    var aiEngine = DEFAULT_AI_ENGINE
    var customOpenAiHost = ""
    var customEngineServer = ""
    var customEngineToken = ""
    var customEnginePrompts = ""

    override fun getState(): AutoDevSettingsState {
        return this
    }

    override fun loadState(state: AutoDevSettingsState) {
        if (customEnginePrompts == "") {
            customEnginePrompts = Json.encodeToString(PromptConfig.default())
        }

        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): AutoDevSettingsState? {
            return ApplicationManager.getApplication().getService(AutoDevSettingsState::class.java)
        }
    }

}
