package cc.unitmesh.devti.settings.coder

import cc.unitmesh.devti.settings.MAX_TOKEN_LENGTH
import cc.unitmesh.devti.settings.ResponseType
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.coderSetting: AutoDevCoderSettingService
    get() = service<AutoDevCoderSettingService>()

@Service(Service.Level.PROJECT)
@State(name = "AutoDevCoderSettings", storages = [Storage("autodev-coder.xml")])
class AutoDevCoderSettingService(
    val project: Project,
) : SimplePersistentStateComponent<AutoDevCoderSettingService.AutoDevCoderSettings>(AutoDevCoderSettings()) {
    fun modify(action: (AutoDevCoderSettings) -> Unit) {
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class AutoDevCoderSettings : AdProjectSettingsBase<AutoDevCoderSettings>() {
        var recordingInLocal by property(false)
        var disableAdvanceContext by property(false)
        var inEditorCompletion by property(false)
        var noChatHistory by property(false)
        var enableRenameSuggestion by property(false)

        var useCustomAIEngineWhenInlayCodeComplete by property(false)
        var maxTokenLengthParam: String by property(MAX_TOKEN_LENGTH.toString()) { it.isEmpty() }
        var delaySecondsParam: String by property("") { it.isEmpty() }
        var customEngineResponseTypeParam by property(ResponseType.SSE.name) { it.isEmpty() }
        var customEngineResponseFormatParam by property("") { it.isEmpty() }
        var customEngineRequestBodyFormatParam by property("") { it.isEmpty() }
        var customEngineServerParam by property("") { it.isEmpty() }
        var customEngineTokenParam by property("") { it.isEmpty() }
        var customEnginePrompt by property("") { it.isEmpty() }

        override fun copy(): AutoDevCoderSettings {
            val state = AutoDevCoderSettings()
            state.copyFrom(this)
            return state
        }
    }
}