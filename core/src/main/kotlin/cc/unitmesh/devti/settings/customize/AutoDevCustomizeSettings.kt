package cc.unitmesh.devti.settings.customize

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.customizeSetting: AutoDevCustomizeSettings
    get() = service<AutoDevCustomizeSettings>()

@Service(Service.Level.PROJECT)
@State(name = "AutoDevCustomizeSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class AutoDevCustomizeSettings(
    val project: Project,
) : SimplePersistentStateComponent<AutoDevCustomizeSettings.CustomizeProjectSettings>(CustomizeProjectSettings()) {
    val enableCustomRag: Boolean get() = state.enableCustomRag
    val customPrompts: String get() = state.customPrompts

    /**
     *  Use [cc.unitmesh.devti.agent.model.CustomAgentConfig.loadFromProject]
     */
    val ragsJsonConfig: String get() = state.agentJsonConfig

    fun modify(action: (CustomizeProjectSettings) -> Unit) {
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class CustomizeProjectSettings : AdProjectSettingsBase<CustomizeProjectSettings>() {
        var enableCustomRag by property(false)
        var agentJsonConfig by property("") { it.isEmpty() }
        var customPrompts by property("") { it.isEmpty() }

        override fun copy(): CustomizeProjectSettings {
            val state = CustomizeProjectSettings()
            state.copyFrom(this)
            return state
        }
    }
}