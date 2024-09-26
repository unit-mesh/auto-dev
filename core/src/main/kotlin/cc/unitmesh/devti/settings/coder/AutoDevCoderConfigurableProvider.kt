package cc.unitmesh.devti.settings.coder

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project

class AutoDevCoderConfigurableProvider (private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return AutoDevCoderConfigurable(project)
    }
}