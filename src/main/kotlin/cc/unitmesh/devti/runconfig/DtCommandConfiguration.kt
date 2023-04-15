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

    lateinit var runConfigure: DevtiConfigure

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        if (!this::runConfigure.isInitialized) {
            runConfigure = DevtiConfigure.getDefault()
        }

        return DtRunState(environment, this, runConfigure)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DtSettingsEditor(project)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        if (this::runConfigure.isInitialized) {
            element.writeString("githubToken", runConfigure.githubToken)
            element.writeString("openAiApiKey", runConfigure.openAiApiKey)
            element.writeString("openAiEngine", runConfigure.openAiEngine)
            element.writeString("openAiMaxTokens", runConfigure.openAiMaxTokens.toString())
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        if (this::runConfigure.isInitialized) {
            element.readString("githubToken")?.let { runConfigure.githubToken = it }
            element.readString("openAiApiKey")?.let { runConfigure.openAiApiKey = it }
            element.readString("openAiEngine")?.let { runConfigure.openAiEngine = it }
            element.readString("openAiMaxTokens")?.let { runConfigure.openAiMaxTokens = it.toInt() }
        }
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
