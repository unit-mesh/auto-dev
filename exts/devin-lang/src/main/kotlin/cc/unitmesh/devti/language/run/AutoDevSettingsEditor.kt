package cc.unitmesh.devti.language.run

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

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
        myScriptSelector!!.text = configuration.getScriptPath()
    }

    override fun applyEditorTo(configuration: AutoDevConfiguration) {
        configuration.setScriptPath(myScriptSelector!!.text)
    }
}