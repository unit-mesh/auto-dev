package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.prompt.openai.DtOpenAIVersion
import cc.unitmesh.devti.runconfig.config.AiCopilotConfiguration
import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AiCopilotSettingsEditor(project: Project) : BaseSettingsEditor<AiCopilotConfiguration>(project) {
    override fun resetEditorFrom(configuration: AiCopilotConfiguration) {
        aiApiToken.text = configuration.options.openAiApiKey()
        aiEngineVersion.selectedIndex = configuration.options.aiEngineVersion()
        openAiMaxTokens = configuration.options.aiMaxTokens()
        maxTokens.text = openAiMaxTokens.toString()
    }

    override fun applyEditorTo(configuration: AiCopilotConfiguration) {
        configuration.setOpenAiApiKey(aiApiToken.text)
        configuration.setAiVersion(DtOpenAIVersion.fromIndex(aiEngineVersion.selectedIndex))
        configuration.setAiMaxTokens(openAiMaxTokens)
    }
}
