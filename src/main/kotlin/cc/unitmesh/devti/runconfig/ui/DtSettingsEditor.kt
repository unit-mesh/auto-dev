package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : SettingsEditor<DtCommandConfiguration>() {
    private val command: DtCommandLineEditor = DtCommandLineEditor(project, DtCommandCompletionProvider());

    override fun resetEditorFrom(configuration: DtCommandConfiguration) {
//        command.text = configuration
    }

    override fun applyEditorTo(configuration: DtCommandConfiguration) {
//        configuration.command = command.text
    }

    @Suppress("UnstableApiUsage")
    override fun createEditor(): JComponent = panel {
        row("DevTi:") {
            cell(command)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
        }
    }
}
