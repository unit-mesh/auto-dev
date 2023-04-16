package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.DevtiConfigure
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : SettingsEditor<DtRunConfiguration>() {
    private var completionProvider = DtCommandCompletionProvider()
    private var openAiApiKey: String = ""
    private var openAiEngine: String = "gpt-3.5-turbo"
    private var openAiMaxTokens: Int = 4096

    private var panel: JComponent? = null

    private val githubInput = DtCommandLineEditor(project, completionProvider)
    override fun createEditor(): JComponent = panel {
        row {
            label("Github Token:")
            fullWidthCell(githubInput)
        }

        row("OpenAI API Key:") {
            textField()
                .horizontalAlign(HorizontalAlign.FILL).resizableColumn()
                .bindText(::openAiApiKey)
        }

        row("API Engine:") {
            comboBox(listOf("gpt-3.5-turbo")).bindItem(::openAiEngine)
        }

        row("Max Token") {
            intTextField(0..32768).bindIntText(::openAiMaxTokens)
        }
    }.also {
        panel = it
    }

    override fun resetEditorFrom(configuration: DtRunConfiguration) {
        val configure = configuration.options.toConfigure()
        githubInput.text = configure.githubToken
        openAiApiKey = configure.openAiApiKey
        openAiEngine = configure.openAiEngine
        openAiMaxTokens = configure.openAiMaxTokens
    }

    override fun applyEditorTo(configuration: DtRunConfiguration) {
        logger.warn("github text:${githubInput.text}")
        configuration.setOptions(
            DevtiConfigure(
                githubInput.text,
                openAiApiKey,
                openAiEngine,
                openAiMaxTokens,
                0.0f
            )
        )
    }

    companion object {
        val logger = Logger.getInstance(DtSettingsEditor::class.java)
    }
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}
