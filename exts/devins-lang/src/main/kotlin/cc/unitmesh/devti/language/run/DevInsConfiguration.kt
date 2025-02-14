package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.language.DevInBundle
import cc.unitmesh.devti.language.DevInIcons
import cc.unitmesh.devti.runconfig.config.readString
import cc.unitmesh.devti.runconfig.config.writeString
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

    var showConsole: Boolean = true
}
