package cc.unitmesh.devti.settings.custom

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.teamPromptsSettings: TeamPromptsProjectSettingsService
    get() = service<TeamPromptsProjectSettingsService>()

@Service(Service.Level.PROJECT)
@State(name = "AutoDevTeamPromptProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class TeamPromptsProjectSettingsService(
    val project: Project,
) : SimplePersistentStateComponent<TeamPromptsProjectSettingsService.TeamPromptsSettings>(TeamPromptsSettings()) {
    fun modify(action: (TeamPromptsSettings) -> Unit) {
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class TeamPromptsSettings : AdProjectSettingsBase<TeamPromptsSettings>() {
        var teamPromptsDir by property("prompts") { it.isEmpty() }

        override fun copy(): TeamPromptsSettings {
            val state = TeamPromptsSettings()
            state.copyFrom(this)
            return state
        }
    }
}