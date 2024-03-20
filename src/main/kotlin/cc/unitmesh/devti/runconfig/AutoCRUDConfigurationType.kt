package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import cc.unitmesh.devti.runconfig.options.AutoCRUDConfigurationOptions
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class AutoCRUDConfigurationType :
    ConfigurationTypeBase(
        AutoCRUDConfigurationFactory.ID,
        AutoDevBundle.message("name"),
        "AutoCRUD generator",
        AutoDevIcons.STORY
    ) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(AutoCRUDConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): AutoCRUDConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(AutoCRUDConfigurationType::class.java)
    }
}

class AutoCRUDConfigurationFactory(type: AutoCRUDConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        AutoCRUDConfiguration(project, "AutoDev", this)

    override fun getOptionsClass(): Class<out BaseState?> = AutoCRUDConfigurationOptions::class.java

    companion object {
        const val ID: String = "AutoCRUDRunConfiguration"
    }
}
