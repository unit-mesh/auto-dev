package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.DevtiIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class DtCommandConfigurationType :
    ConfigurationTypeBase("DtCommandConfigurationType", "DevTi", "DevTi generator", DevtiIcons.STORY) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(DtConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): DtCommandConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(DtCommandConfigurationType::class.java)
    }
}

class DtConfigurationFactory(type: DtCommandConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DtCommandConfiguration(project, "DevTi", this)
    }

    companion object {
        const val ID: String = "DevTi Gen"
    }
}
