package cc.unitmesh.devti.settings.devops

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

class AutoDevDevOpsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return DevOpsConfigurable(project)
    }
}

class DevOpsConfigurable(project: Project) : BoundConfigurable(AutoDevBundle.message("settings.autodev.devops")) {
    override fun createPanel(): DialogPanel {
        return panel {
            row {
                text("Hello, AutoDev 2.0")
            }
        }
    }
}
