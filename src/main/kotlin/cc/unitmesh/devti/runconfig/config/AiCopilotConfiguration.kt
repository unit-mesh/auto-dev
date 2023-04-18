package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.runconfig.options.OpenAIConfigureOptions
import cc.unitmesh.devti.runconfig.ui.AiCopilotSettingsEditor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class AiCopilotConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    RunConfigurationBase<OpenAIConfigureOptions>(project, factory, name) {

    public override fun getOptions(): OpenAIConfigureOptions {
        return super.getOptions() as OpenAIConfigureOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return AiCopilotSettingsEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return CopilotState(environment, this, project)
    }

    override fun checkRunnerSettings(
        runner: ProgramRunner<*>,
        runnerSettings: RunnerSettings?,
        configurationPerRunnerSettings: ConfigurationPerRunnerSettings?
    ) {
        super.checkRunnerSettings(runner, runnerSettings, configurationPerRunnerSettings)
    }
}
