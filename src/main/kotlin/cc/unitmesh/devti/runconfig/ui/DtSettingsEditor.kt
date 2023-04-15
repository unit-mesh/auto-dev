package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DevtiConfigure
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : SettingsEditor<DtRunConfiguration>() {
    private var githubToken: String = ""
    private var openAiApiKey: String = ""
    private var openAiEngine: String = "gpt-3.5-turbo"
    private var openAiMaxTokens: Int = 4096

    @Suppress("UnstableApiUsage")
    private val myPanel = panel {
        row("Github Token:") {
            textField()
                .bindText(::githubToken)
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("OpenAI API Key:") {
            textField()
                .bindText(::openAiApiKey)
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
        }

        row("API Engine:") {
            comboBox(listOf("gpt-3.5-turbo")).bindItem(::openAiEngine)
        }

        row("Max Token") {
            intTextField(0..32768).bindIntText(::openAiMaxTokens)
        }
    }

    override fun resetEditorFrom(configuration: DtRunConfiguration) {
        val configure = configuration.options.toConfigure()
        githubToken = configure.githubToken
        openAiApiKey = configure.openAiApiKey
        openAiEngine = configure.openAiEngine
        openAiMaxTokens = configure.openAiMaxTokens
    }

    override fun applyEditorTo(configuration: DtRunConfiguration) {
        configuration.setOptions(
            DevtiConfigure(
                githubToken,
                openAiApiKey,
                openAiEngine,
                openAiMaxTokens,
                0.0f
            )
        )
    }

    override fun createEditor(): JComponent = myPanel
}
