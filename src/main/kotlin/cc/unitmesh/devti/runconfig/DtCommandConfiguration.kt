package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.runconfig.ui.DtSettingsEditor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class DtCommandConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    LocatableConfigurationBase<RunProfileState>(project, factory, name) {

    private lateinit var runConfigure: DtAiConfigure

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DtRunState(environment, this, runConfigure)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DtSettingsEditor(project)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        // todos
    }
}
