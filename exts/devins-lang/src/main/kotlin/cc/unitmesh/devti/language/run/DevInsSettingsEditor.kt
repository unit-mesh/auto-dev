package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.language.DevInBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class DevInsSettingsEditor(val project: Project) : SettingsEditor<DevInsConfiguration>() {
    private val myScriptSelector: TextFieldWithBrowseButton = TextFieldWithBrowseButton()

    init {
        myScriptSelector.addBrowseFolderListener(
            DevInBundle.message("devin.label.choose.file"),
            "",
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
    }

    override fun createEditor(): JComponent = panel {
        row {
            cell(myScriptSelector).fullWidth()
        }
    }

    override fun resetEditorFrom(configuration: DevInsConfiguration) {
        myScriptSelector.text = configuration.getScriptPath()
    }

    override fun applyEditorTo(configuration: DevInsConfiguration) {
        configuration.setScriptPath(myScriptSelector.text)
    }
}