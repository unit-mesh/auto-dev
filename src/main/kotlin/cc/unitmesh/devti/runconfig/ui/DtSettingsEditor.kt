package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DtAiConfigure
import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent
import com.intellij.ui.dsl.builder.*

class DtSettingsEditor(project: Project) : SettingsEditor<DtCommandConfiguration>() {
    val configure = DtAiConfigure.getDefault()

    override fun resetEditorFrom(configuration: DtCommandConfiguration) {
//        command.text = configuration
    }

    override fun applyEditorTo(configuration: DtCommandConfiguration) {
//        configuration.command = command.text
    }

    @Suppress("UnstableApiUsage")
    override fun createEditor(): JComponent = panel {
        row("Github Token") {
            textField()
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("OpenAI API Key") {
            textField().horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("API Engine") {
            comboBox(listOf("gpt-3.5-turbo"))
        }
    }
}
