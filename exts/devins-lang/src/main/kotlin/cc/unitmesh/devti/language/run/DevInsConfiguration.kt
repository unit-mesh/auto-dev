package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.DevInIcons
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jdom.Element
import javax.swing.Icon

class DevInsConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<ConfigurationFactory>(project, factory, name) {
    override fun getIcon(): Icon = DevInIcons.DEFAULT

    private var myScriptPath = ""
    private val SCRIPT_PATH_TAG: String = "SCRIPT_PATH"
    private val SHOW_CONSOLE_TAG: String = "SHOW_CONSOLE"

    private var varMap: Map<String, String> = mutableMapOf()

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DevInsRunConfigurationProfileState(project, this)
    }

    override fun checkConfiguration() {
        if (!FileUtil.exists(myScriptPath)) {
            throw RuntimeConfigurationError(DevInBundle.message("devin.run.error.script.not.found"))
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString(SCRIPT_PATH_TAG, myScriptPath)
        element.writeString(SHOW_CONSOLE_TAG, showConsole.toString())
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        myScriptPath = element.readString(SCRIPT_PATH_TAG) ?: ""
        showConsole = element.readString(SHOW_CONSOLE_TAG)?.toBoolean() != false
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = DevInsSettingsEditor(project)

    fun getScriptPath(): String = myScriptPath

    fun setScriptPath(scriptPath: String) {
        myScriptPath = scriptPath.trim { it <= ' ' }
    }

    fun getVariables(): Map<String, String> = varMap

    fun setVariables(variables: Map<String, String>) {
        varMap = variables
    }

    var showConsole: Boolean = true

    companion object {
        fun mapStringToMap(varMapString: String) = varMapString
            .removePrefix("{")
            .removeSuffix("}")
            .split(", ")
            .map { it.split("=") }
            .filter { it.size >= 2 }
            .associate { it[0] to it[1] }
            .toMutableMap()
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
