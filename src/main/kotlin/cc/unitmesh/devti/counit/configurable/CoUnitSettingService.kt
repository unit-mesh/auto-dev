package cc.unitmesh.devti.counit.configurable

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.customRagSettings: CoUnitProjectSettingsService
    get() = service<CoUnitProjectSettingsService>()

@Service(Service.Level.PROJECT)
@State(name = "CoUnitProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CoUnitProjectSettingsService(
    val project: Project,
) : SimplePersistentStateComponent<CoUnitProjectSettingsService.CoUnitProjectSettings>(CoUnitProjectSettings()) {
    val enableCustomRag: Boolean get() = state.enableCustomRag
    val serverAddress: String get() = state.serverAddress
    val ragsJsonConfig: String get() = state.agentJsonConfig

    fun modify(action: (CoUnitProjectSettings) -> Unit) {
        // todo
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class CoUnitProjectSettings : AdProjectSettingsBase<CoUnitProjectSettings>() {
        var enableCustomRag by property(false)
        var serverAddress by property("http://localhost:8765/api/") { it.isEmpty() }
        var agentJsonConfig by property("") { it.isEmpty() }

        override fun copy(): CoUnitProjectSettings {
            val state = CoUnitProjectSettings()
            state.copyFrom(this)
            return state
        }
    }
}