package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DevtiConfigure
import cc.unitmesh.devti.runconfig.DtCommandConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import java.awt.TextField
import javax.swing.JComponent
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindIntText

class DtSettingsEditor(project: Project) : SettingsEditor<DtCommandConfiguration>() {
    var configure = DevtiConfigure.getDefault()
    override fun resetEditorFrom(configuration: DtCommandConfiguration) {
        configure = configuration.runConfigure
    }

    override fun applyEditorTo(configuration: DtCommandConfiguration) {
        configuration.runConfigure = configure
    }

    @Suppress("UnstableApiUsage")
    override fun createEditor(): JComponent = panel {
        row("Github Token:") {
            textField()
                .bindText(configure::githubToken)
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("OpenAI API Key:") {
            textField()
                .bindText(configure::openAiApiKey)
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("API Engine:") {
            comboBox(listOf("gpt-3.5-turbo")).bindItem(configure::openAiEngine)
        }

        row("Max Token") {
            intTextField(0..32768).bindIntText(configure::openAiMaxTokens)
        }
    }
}
