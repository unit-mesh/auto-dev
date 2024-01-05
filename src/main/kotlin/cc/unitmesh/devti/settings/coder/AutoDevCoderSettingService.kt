package cc.unitmesh.devti.settings.coder

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.coderSetting: AutoDevCoderSettingService
    get() = service<AutoDevCoderSettingService>()

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

        var explainCode: String by property("Explain \$lang code") { it.isEmpty() }
        var refactorCode: String by property("Refactor the given \$lang code") { it.isEmpty() }
        var fixIssueCode: String by property("Help me fix this issue") { it.isEmpty() }
        var generateTest: String by property("Generate test for \$lang code") { it.isEmpty() }

        override fun copy(): AutoDevCoderSettings {
            val state = AutoDevCoderSettings()
            state.copyFrom(this)
            return state
        }
    }
}