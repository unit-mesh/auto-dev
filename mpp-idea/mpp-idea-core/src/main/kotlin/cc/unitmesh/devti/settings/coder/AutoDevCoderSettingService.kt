package cc.unitmesh.devti.settings.coder

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
        var enableExportAsMcpServer by property(false)
        var enableObserver by property(true)
        var enableAutoRepairDiff by property(false)
        var inEditorCompletion by property(false)
        var noChatHistory by property(false)
        var trimCodeBeforeSend by property(false)
        var enableRenameSuggestion by property(false)
        var enableAutoRunTerminal by property(false)
        var enableAutoLintCode by property(true)
        var enableRenderWebview by property(false)
        var enableAutoScrollInSketch by property(false)
        var enableDiffViewer by property(true)
        var teamPromptsDir by property("prompts") { it.isEmpty() }
        var enableHomeSpunGitIgnore by property(true)

        override fun copy(): AutoDevCoderSettings {
            val state = AutoDevCoderSettings()
            state.copyFrom(this)
            return state
        }
    }
}