package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.runconfig.ui.DtSettingsEditor
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class DtRunConfiguration(project: Project, name: String, factory: ConfigurationFactory) :
    RunConfigurationBase<DtRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): DtRunConfigurationOptions {
        return super.getOptions() as DtRunConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DtSettingsEditor(project)
    }

    fun setOptions(configure: DevtiConfigure) {
        this.options.setFrom(configure)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val commandLine = GeneralCommandLine("echo", "hello world")
                val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
                ProcessTerminatedListener.attach(processHandler)
                return processHandler
            }
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        val runConfigure = this.options.toConfigure()

        element.writeString("githubToken", runConfigure.githubToken)
        element.writeString("openAiApiKey", runConfigure.openAiApiKey)
        element.writeString("openAiEngine", runConfigure.openAiEngine)
        element.writeString("openAiMaxTokens", runConfigure.openAiMaxTokens.toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        val runConfigure = DevtiConfigure("", "", "", 4096, 0.0f)

        element.readString("githubToken")?.let { runConfigure.githubToken = it }
        element.readString("openAiApiKey")?.let { runConfigure.openAiApiKey = it }
        element.readString("openAiEngine")?.let { runConfigure.openAiEngine = it }
        element.readString("openAiMaxTokens")?.let { runConfigure.openAiMaxTokens = it.toInt() }

        this.options.setFrom(runConfigure)
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
