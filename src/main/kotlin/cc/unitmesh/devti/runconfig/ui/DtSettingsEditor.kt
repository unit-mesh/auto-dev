package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class DtSettingsEditor(project: Project) : SettingsEditor<AutoCRUDConfiguration>() {
    protected var completionProvider = DtCommandCompletionProvider()

    private var githubRepo = DtCommandLineEditor(project, completionProvider)
    private var storyId = DtCommandLineEditor(project, completionProvider)

    protected var panel: JComponent? = null

    override fun createEditor(): JComponent = panel {
        row("GitHub Project (owner/repo)") {
            cell { githubRepo() }
        }

        // story id
        row("Story ID:") {
            cell { storyId() }
        }
    }.also {
        panel = it
    }

    override fun resetEditorFrom(configuration: AutoCRUDConfiguration) {
        githubRepo.text = configuration.options.githubRepo()
        storyId.text = configuration.options.storyId()
    }

    override fun applyEditorTo(configuration: AutoCRUDConfiguration) {
        configuration.setGithubRepo(githubRepo.text)
        configuration.setStoryId(storyId.text)
    }
}

