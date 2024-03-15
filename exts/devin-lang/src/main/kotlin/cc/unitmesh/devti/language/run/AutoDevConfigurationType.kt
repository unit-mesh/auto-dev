package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.language.DevInLanguage
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class AutoDevConfigurationType : SimpleConfigurationType(
"AutoDevConfigurationType",
    DevInLanguage.INSTANCE.id,
    AutoDevBundle.message("line.marker.run.0", DevInLanguage.INSTANCE.id),
    NotNullLazyValue.lazy { DevInIcons.DEFAULT }
) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return AutoDevConfiguration(project, "AutoDev", AutoDevConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): AutoDevConfigurationType {
            return findConfigurationType(AutoDevConfigurationType::class.java)
        }
    }

}

class AutoDevConfigurationFactory(type: AutoDevConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        AutoDevConfiguration(project, "AutoDev", this)

    override fun getOptionsClass(): Class<out BaseState?> = AutoDevConfigurationOptions::class.java

    companion object {
        const val ID: String = "AutoDevRunConfiguration"
    }
}

class AutoDevConfigurationOptions : ModuleBasedConfigurationOptions() {

}
