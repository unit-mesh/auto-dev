package cc.unitmesh.devti.settings.custom

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

val Project.promptLibrarySettings: PromptLibraryProjectSettingsService
    get() = service<PromptLibraryProjectSettingsService>()

@State(name = "AutoDevPromptLibraryProjectSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class PromptLibraryProjectSettingsService(
    val project: Project,
) : SimplePersistentStateComponent<PromptLibraryProjectSettingsService.PromptLibrarySettings>(PromptLibrarySettings()) {
    fun modify(action: (PromptLibrarySettings) -> Unit) {
        action(state)
    }

    abstract class AdProjectSettingsBase<T : AdProjectSettingsBase<T>> : BaseState() {
        abstract fun copy(): T
    }

    class PromptLibrarySettings : AdProjectSettingsBase<PromptLibrarySettings>() {
        var libraryDirectory by property("prompts") { it.isEmpty() }

        override fun copy(): PromptLibrarySettings {
            val state = PromptLibrarySettings()
            state.copyFrom(this)
            return state
        }
    }
}