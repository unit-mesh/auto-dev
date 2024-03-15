package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.execution.configurations.*
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class AutoDevConfigurationType : ConfigurationTypeBase(
    AutoDevConfigurationFactory.ID,
    AutoDevBundle.message("autodev.devti"),
    "AutoDev DevIn Language executor",
    AutoDevIcons.AI_COPILOT
) {
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

class AutoDevConfigurationOptions : ModuleBasedConfigurationOptions() {

}
