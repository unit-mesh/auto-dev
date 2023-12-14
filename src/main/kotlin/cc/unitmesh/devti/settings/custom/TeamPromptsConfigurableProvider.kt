package cc.unitmesh.devti.settings.custom

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class TeamPromptsConfigurableProvider (private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return PromptLibraryConfigurable(project)
    }
}