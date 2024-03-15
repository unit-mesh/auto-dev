package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.options.AutoCRUDConfigurationOptions
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.Icon

class AutoDevConfigurationType : ConfigurationTypeBase(
    AutoDevConfigurationFactory.ID,
    AutoDevBundle.message("autodev.crud"),
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

class AutoDevConfigurationFactory(autoDevConfigurationType: AutoDevConfigurationType) : ConfigurationFactory() {
    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        AutoDevConfiguration(project, "AutoDev", this)

    override fun getOptionsClass(): Class<out BaseState?> = AutoDevConfigurationOptions::class.java

    companion object {
        const val ID: String = "AutoCRUDRunConfiguration"
    }
}

class AutoDevConfiguration(project: Project, s: String, autoDevConfigurationFactory: AutoDevConfigurationFactory) :
    RunConfiguration {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        TODO("Not yet implemented")
    }

    override fun getIcon(): Icon? {
        TODO("Not yet implemented")
    }

    override fun clone(): RunConfiguration {
        TODO("Not yet implemented")
    }

    override fun getFactory(): ConfigurationFactory? {
        TODO("Not yet implemented")
    }

    override fun setName(name: String?) {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }

    override fun getProject(): Project {
        TODO("Not yet implemented")
    }

}

class AutoDevConfigurationOptions : ModuleBasedConfigurationOptions() {

}
