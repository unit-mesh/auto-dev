package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.prompt.openai.DtOpenAIVersion
import cc.unitmesh.devti.runconfig.DtRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : BaseSettingsEditor<DtRunConfiguration>(project) {
    private val githubToken = DtCommandLineEditor(project, completionProvider)
    private val githubRepo = DtCommandLineEditor(project, completionProvider)
    private val storyId = DtCommandLineEditor(project, completionProvider)

    override fun createEditor(): JComponent = panel {
        row("Github Token:") {
            fullWidthCell(githubToken)
        }

        row ("GitHub Project (owner/repo)") {
            fullWidthCell(githubRepo)
        }

        // story id
        row ("Story ID:") {
            fullWidthCell(storyId)
        }

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

    override fun resetEditorFrom(configuration: DtRunConfiguration) {
        githubToken.text = configuration.options.githubToken()
        githubRepo.text = configuration.options.githubRepo()
        aiApiToken.text = configuration.options.openAiApiKey()
        aiEngineVersion.selectedIndex = configuration.options.aiEngineVersion()
        openAiMaxTokens = configuration.options.aiMaxTokens()
        maxTokens.text = openAiMaxTokens.toString()
        storyId.text = configuration.options.storyId()
    }

    override fun applyEditorTo(configuration: DtRunConfiguration) {
        configuration.setGithubToken(githubToken.text)
        configuration.setGithubRepo(githubRepo.text)
        configuration.setOpenAiApiKey(aiApiToken.text)
        configuration.setAiVersion(DtOpenAIVersion.fromIndex(aiEngineVersion.selectedIndex))
        configuration.setAiMaxTokens(openAiMaxTokens)
        configuration.setStoryId(storyId.text)
    }
}
