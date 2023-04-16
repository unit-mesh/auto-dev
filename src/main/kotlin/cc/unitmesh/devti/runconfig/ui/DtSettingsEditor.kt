package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.ai.OpenAIVersion
import cc.unitmesh.devti.runconfig.config.DevtiCreateStoryConfigure
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : SettingsEditor<DtRunConfiguration>() {
    private var completionProvider = DtCommandCompletionProvider()
    private var openAiMaxTokens: Int = DevtiCreateStoryConfigure.DEFAULT_OPEN_AI_MAX_TOKENS

    private var panel: JComponent? = null

    private val githubInput = DtCommandLineEditor(project, completionProvider)
    private val aiApiToken = DtCommandLineEditor(project, completionProvider)
    private var engineVersion = ComboBox<OpenAIVersion>().apply {
        OpenAIVersion.values()
            .sortedBy { it.index }
            .forEach { addItem(it) }
    }
    private val maxTokens = DtCommandLineEditor(project, completionProvider).apply {
        setText(openAiMaxTokens.toString())
    }

    override fun createEditor(): JComponent = panel {
        row {
            label("Github Token:")
            fullWidthCell(githubInput)
        }

        row("API Engine:") {
            comboBox(listOf("OpenAI"))
        }

        row("AI API Key:") {
            fullWidthCell(aiApiToken)
        }

        row("API Engine:") {
            cell(engineVersion)
        }

        row("Max Tokens") {
            fullWidthCell(maxTokens)
        }
    }.also {
        panel = it
    }

    override fun resetEditorFrom(configuration: DtRunConfiguration) {
        val configure = configuration.options.toConfigure()
        githubInput.text = configure.githubToken
        aiApiToken.text = configure.openAiApiKey
        engineVersion.selectedIndex = configure.aiVersion.index
        openAiMaxTokens = configure.aiMaxTokens
    }

    override fun applyEditorTo(configuration: DtRunConfiguration) {
        logger.warn("github text:${githubInput.text}")
        configuration.setOptions(
            DevtiCreateStoryConfigure(
                githubInput.text,
                aiApiToken.text,
                OpenAIVersion.fromIndex(engineVersion.selectedIndex),
                openAiMaxTokens
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
