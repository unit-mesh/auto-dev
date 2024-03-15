package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.runconfig.config.readString
import cc.unitmesh.devti.runconfig.config.writeString
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.panel
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import javax.swing.Icon
import javax.swing.JComponent

class AutoDevConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<ConfigurationFactory>(project, factory, name) {
    override fun getIcon(): Icon = AutoDevIcons.AI_COPILOT
    private var myScriptPath = ""
    private val SCRIPT_PATH_TAG: @NonNls String = "SCRIPT_PATH"

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return DevInRunConfigurationProfileState(project, this)
    }

    override fun checkConfiguration() {
        if (!FileUtil.exists(myScriptPath)) {
            throw RuntimeConfigurationError(AutoDevBundle.message("devin.run.error.script.not.found"))
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.writeString(SCRIPT_PATH_TAG, myScriptPath)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        myScriptPath = element.readString(SCRIPT_PATH_TAG) ?: ""
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return AutoDevSettingsEditor(project)
    }

    fun setScriptPath(scriptPath: String) {
        myScriptPath = scriptPath.trim { it <= ' ' }
    }

    fun getScriptPath(): @NlsSafe String {
        return myScriptPath
    }
}

class AutoDevSettingsEditor(val project: Project) : SettingsEditor<AutoDevConfiguration>() {
    private val myScriptSelector: TextFieldWithBrowseButton? = null

    override fun createEditor(): JComponent = panel {
        myScriptSelector?.addBrowseFolderListener(
            AutoDevBundle.message("devin.label.choose.file"),
            "",
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
    }

    override fun resetEditorFrom(configuration: AutoDevConfiguration) {
        myScriptSelector!!.setText(configuration.getScriptPath())
    }

    override fun applyEditorTo(configuration: AutoDevConfiguration) {
        configuration.setScriptPath(myScriptSelector!!.text)
    }
}
