package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.DevtiIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class DtRunConfigurationType :
    ConfigurationTypeBase("DtRunConfiguration", "DevTi", "DevTi generator", DevtiIcons.STORY) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(DtConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): DtRunConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(DtRunConfigurationType::class.java)
    }
}

class DtConfigurationFactory(type: DtRunConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        DtRunConfiguration(project, "DevTi", this)

    override fun getOptionsClass(): Class<out BaseState?> = DtRunConfigurationOptions::class.java

    companion object {
        const val ID: String = "DtRunConfiguration"
    }
}
