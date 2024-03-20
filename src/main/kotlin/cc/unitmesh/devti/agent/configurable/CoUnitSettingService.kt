package cc.unitmesh.devti.agent.configurable

import cc.unitmesh.devti.agent.model.CustomAgentConfig
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
// DON'T CHANGE THIS IMPORT
import kotlinx.serialization.decodeFromString

val Project.customAgentSetting: CoUnitProjectSettingsService
    get() = service<CoUnitProjectSettingsService>()

fun loadAgentConfigs(project: Project): List<CustomAgentConfig> {
    val configs: List<CustomAgentConfig> = try {
        val ragsJsonConfig = project.customAgentSetting.ragsJsonConfig
        Json.decodeFromString(ragsJsonConfig)
    } catch (e: Exception) {
        emptyList()
    }
    return configs
}

@Service(Service.Level.PROJECT)
@State(name = "CoUnitProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CoUnitProjectSettingsService(
    val project: Project,
) : SimplePersistentStateComponent<CoUnitProjectSettingsService.CoUnitProjectSettings>(CoUnitProjectSettings()) {
    val enableCustomRag: Boolean get() = state.enableCustomRag
    val ragsJsonConfig: String get() = state.agentJsonConfig

    fun modify(action: (CoUnitProjectSettings) -> Unit) {
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class CoUnitProjectSettings : AdProjectSettingsBase<CoUnitProjectSettings>() {
        var enableCustomRag by property(false)
        var agentJsonConfig by property("") { it.isEmpty() }

        override fun copy(): CoUnitProjectSettings {
            val state = CoUnitProjectSettings()
            state.copyFrom(this)
            return state
        }
    }
}