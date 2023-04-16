package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.ai.OpenAIVersion
import cc.unitmesh.devti.language.StoryConfig
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import cc.unitmesh.devti.runconfig.ui.DtSettingsEditor
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class DtRunConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    RunConfigurationBase<DtRunConfigurationOptions>(project, factory, name) {

    private var storyConfig: StoryConfig? = null

    public override fun getOptions(): DtRunConfigurationOptions {
        return super.getOptions() as DtRunConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DtSettingsEditor(project)
    }

    fun setOptions(configure: DevtiCreateStoryConfigure) {
        this.options.setFrom(configure)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val createStoryConfigure = this.options.toConfigure()
        createStoryConfigure.storyConfig = this.storyConfig
        return DtRunState(environment, this, createStoryConfigure)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        val runConfigure = this.options.toConfigure()

        element.writeString("githubToken", runConfigure.githubToken)
        element.writeString("openAiApiKey", runConfigure.openAiApiKey)
        element.writeString("aiEngineVersion", runConfigure.aiVersion.index.toString())
        element.writeString("aiMaxTokens", runConfigure.aiMaxTokens.toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        val runConfigure = DevtiCreateStoryConfigure.getDefault()

        element.readString("githubToken")?.let { runConfigure.githubToken = it }
        element.readString("openAiApiKey")?.let { runConfigure.openAiApiKey = it }
        element.readString("aiEngineVersion")?.let { runConfigure.aiVersion = OpenAIVersion.values()[it.toInt()] }
        element.readString("aiMaxTokens")?.let { runConfigure.aiMaxTokens = it.toInt() }

        this.options.setFrom(runConfigure)
    }

    fun setStoryConfig(storyConfig: StoryConfig?) {
        this.storyConfig = storyConfig
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
