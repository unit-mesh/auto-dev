package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.prompt.openai.DtOpenAIConfig.DEFAULT_OPEN_AI_MAX_TOKENS
import cc.unitmesh.devti.prompt.openai.DtOpenAIVersion
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

abstract class BaseSettingsEditor<T>(project: Project) : SettingsEditor<T>() {
    protected var completionProvider = DtCommandCompletionProvider()
    protected var openAiMaxTokens: Int = DEFAULT_OPEN_AI_MAX_TOKENS

    protected var panel: JComponent? = null

    protected val aiApiToken = DtCommandLineEditor(project, completionProvider)

    protected var aiEngineVersion = ComboBox<DtOpenAIVersion>().apply {
        DtOpenAIVersion.values()
            .sortedBy { it.index }
            .forEach { addItem(it) }
    }
    protected val maxTokens = DtCommandLineEditor(project, completionProvider).apply {
        text = openAiMaxTokens.toString()
    }

    override fun createEditor(): JComponent = panel {
        row("API Engine:") {
            comboBox(listOf("OpenAI"))
        }

        row("AI API Key:") {
            fullWidthCell(aiApiToken)
        }

        row("API Engine:") {
            cell(aiEngineVersion)
        }

        row("Max Tokens") {
            fullWidthCell(maxTokens)
        }
    }.also {
        panel = it
    }
}

fun <T : JComponent> Row.fullWidthCell(component: T): Cell<T> {
    return cell(component)
        .horizontalAlign(HorizontalAlign.FILL)
}
