package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent
import  com.intellij.ui.dsl.builder.*

class DtSettingsEditor(project: Project) : SettingsEditor<DtCommandConfiguration>() {
    override fun resetEditorFrom(configuration: DtCommandConfiguration) {

    }

    override fun applyEditorTo(configuration: DtCommandConfiguration) {

    }

    @Suppress("UnstableApiUsage")
    override fun createEditor(): JComponent = panel {
        row("Github Token:") {
            textField()
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("OpenAI API Key:") {
            textField().horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("API Engine:") {
            comboBox(listOf("gpt-3.5-turbo"))
        }

        row("Max Token") {
            intTextField(0..32768)
        }
    }
}
