package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import cc.unitmesh.devti.runconfig.options.AutoDevConfigurationOptions
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class AutoDevConfigurationType :
    ConfigurationTypeBase("AutoDevConfiguration", "AutoDev", "AutoDev generator", AutoDevIcons.STORY) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(AutoDevConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): AutoDevConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(AutoDevConfigurationType::class.java)
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
