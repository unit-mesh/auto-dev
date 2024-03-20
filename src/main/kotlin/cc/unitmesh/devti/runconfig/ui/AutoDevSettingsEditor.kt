package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.runconfig.config.AutoCRUDConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class AutoDevSettingsEditor(project: Project) : SettingsEditor<AutoCRUDConfiguration>() {
    private var completionProvider = DtCommandCompletionProvider()
    private var panel: JComponent? = null

    private val githubRepo = DtCommandLineEditor(project, completionProvider)
    private val storyId = DtCommandLineEditor(project, completionProvider)

    override fun createEditor(): JComponent = panel {
        row(AutoDevBundle.message("autocrud.settings.githubRepo")) {
            cell(githubRepo).fullWidth()
        }

        row(AutoDevBundle.message("autocrud.settings.storyId")) {
            cell(storyId).fullWidth()
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

