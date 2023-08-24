package cc.unitmesh.devti.settings.configurable

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.coUnitSettings: CoUnitProjectSettingsService
    get() = service<CoUnitProjectSettingsService>()

@State(name = "CoUnitProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CoUnitProjectSettingsService(
    val project: Project,
) : SimplePersistentStateComponent<CoUnitProjectSettingsService.CoUnitProjectSettings>(CoUnitProjectSettings()) {
    val enableCoUnit: Boolean get() = state.enableCoUnit
    val serverAddress: String get() = state.serverAddress

    fun modify(action: (CoUnitProjectSettings) -> Unit) {
        // todo
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class CoUnitProjectSettings : AdProjectSettingsBase<CoUnitProjectSettings>() {
        var enableCoUnit by property(false)
        var serverAddress by property("http://localhost:8765") { it.isEmpty() }

        override fun copy(): CoUnitProjectSettings {
            val state = CoUnitProjectSettings()
            state.copyFrom(this)
            return state
        }
    }
}