package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AutoDevSettingsEditor(project: Project) : SettingsEditor<AutoDevConfiguration>() {
    protected var completionProvider = DtCommandCompletionProvider()

    private val githubRepo = DtCommandLineEditor(project, completionProvider)
    private val storyId = DtCommandLineEditor(project, completionProvider)

    protected var panel: JComponent? = null

    override fun createEditor(): JComponent = panel {
        row("GitHub Project (owner/repo)") {
            fullWidthCell(githubRepo)
        }

        // story id
        row("Story ID:") {
            fullWidthCell(storyId)
        }
    }.also {
        panel = it
    }

    override fun resetEditorFrom(configuration: AutoDevConfiguration) {
        githubRepo.text = configuration.options.githubRepo()
        storyId.text = configuration.options.storyId()
    }

    override fun applyEditorTo(configuration: AutoDevConfiguration) {
        configuration.setGithubRepo(githubRepo.text)
        configuration.setStoryId(storyId.text)
    }
}

