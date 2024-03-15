package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevIcons
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.Icon
import javax.swing.JComponent

class AutoDevConfiguration(project: Project, name: String, factory: AutoDevConfigurationFactory) :
    RunConfigurationBase<AutoDevConfigurationOptions>(project, factory, name) {
    override fun getIcon(): Icon = AutoDevIcons.AI_COPILOT

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        return DevInRunConfigurationProfileState(project, this)
    }

    override fun clone(): RunConfiguration {
        return super.clone() as AutoDevConfiguration
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return AutoDevSettingsEditor(project)
    }
}

class AutoDevSettingsEditor(project: Project) : SettingsEditor<AutoDevConfiguration>() {
    override fun createEditor(): JComponent = panel {

    }

    override fun resetEditorFrom(configuration: AutoDevConfiguration) {
    }

    override fun applyEditorTo(configuration: AutoDevConfiguration) {
    }
}
