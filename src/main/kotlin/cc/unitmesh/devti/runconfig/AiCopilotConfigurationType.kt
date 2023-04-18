package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.DevtiIcons
import cc.unitmesh.devti.runconfig.config.AiCopilotConfiguration
import cc.unitmesh.devti.runconfig.options.OpenAIConfigureOptions
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class AiCopilotConfigurationType :
    ConfigurationTypeBase("AiCopilotConfiguration", "Devti Copilot", "AI Coding", DevtiIcons.AI_COPILOT) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CopilotConfigurationFactory(this))
    }

    companion object {
        fun getInstance(): AiCopilotConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(AiCopilotConfigurationType::class.java)
    }
}

class CopilotConfigurationFactory(type: AiCopilotConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        AiCopilotConfiguration(project, "DevTi Copilot", this)

    override fun getOptionsClass(): Class<out BaseState?> = OpenAIConfigureOptions::class.java

    companion object {
        const val ID: String = "DtRunConfiguration"
    }
}
