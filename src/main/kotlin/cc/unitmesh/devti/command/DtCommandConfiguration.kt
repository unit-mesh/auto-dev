package cc.unitmesh.devti.command

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class DtCommandConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    LocatableConfigurationBase<RunProfileState>(project, factory, name) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }

}