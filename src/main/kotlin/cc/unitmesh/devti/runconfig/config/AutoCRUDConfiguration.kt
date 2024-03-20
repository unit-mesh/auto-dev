package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.runconfig.AutoDevRunProfileState
import cc.unitmesh.devti.runconfig.options.AutoCRUDConfigurationOptions
import cc.unitmesh.devti.runconfig.ui.AutoDevSettingsEditor
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

class AutoCRUDConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    RunConfigurationBase<AutoCRUDConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): AutoCRUDConfigurationOptions {
        return super.getOptions() as AutoCRUDConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return AutoDevSettingsEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return AutoDevRunProfileState(project, options)
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

        element.writeString("githubRepo", options.githubRepo())
        element.writeString("storyId", options.storyId())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        element.readString("githubRepo")?.let { this.options.setGithubRepo(it) }
        element.readString("storyId")?.let { this.options.setStoryId(it) }
    }

    fun setGithubRepo(text: String) {
        this.options.setGithubRepo(text)
    }

    fun setStoryId(text: String) {
        this.options.setStoryId(text)
    }

    fun setStoryConfig(config: AutoDevStory) {
        this.options.setStoryId(config.storyId.toString())
    }
}

fun Element.writeString(name: String, value: String) {
    val opt = Element("option")
    opt.setAttribute("name", name)
    opt.setAttribute("value", value)
    addContent(opt)
}

fun Element.readString(name: String): String? =
    children
        .find { it.name == "option" && it.getAttributeValue("name") == name }
        ?.getAttributeValue("value")
