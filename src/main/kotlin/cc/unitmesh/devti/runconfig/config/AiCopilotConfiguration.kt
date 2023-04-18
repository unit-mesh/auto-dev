package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.prompt.openai.DtOpenAIVersion
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
import org.jdom.Element

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

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        element.writeString("openAiApiKey", options.openAiApiKey())
        element.writeString("aiEngineVersion", options.aiEngineVersion().toString())
        element.writeString("aiMaxTokens", options.aiMaxTokens().toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        element.readString("openAiApiKey")?.let { this.options.setOpenAiApiKey(it) }
        element.readString("aiEngineVersion")?.let { this.options.setAiEngineVersion(it.toInt()) }
        element.readString("aiMaxTokens")?.let { this.options.setAiMaxTokens(it.toInt()) }
    }

    fun setOpenAiApiKey(text: String) {
        this.options.setOpenAiApiKey(text)
    }

    fun setAiVersion(fromIndex: DtOpenAIVersion) {
        this.options.setAiEngineVersion(fromIndex.ordinal)
    }

    fun setAiMaxTokens(openAiMaxTokens: Int) {
        this.options.setAiMaxTokens(openAiMaxTokens)
    }
}
