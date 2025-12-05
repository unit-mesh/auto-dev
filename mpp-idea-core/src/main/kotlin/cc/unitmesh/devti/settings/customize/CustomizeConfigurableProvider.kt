package cc.unitmesh.devti.settings.customize

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class CustomizeConfigurableProvider (private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return CustomizeConfigurable(project)
    }
}