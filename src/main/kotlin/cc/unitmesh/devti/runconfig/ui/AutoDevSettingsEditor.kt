package cc.unitmesh.devti.runconfig.ui

import cc.unitmesh.devti.runconfig.config.AutoDevConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import javax.swing.JComponent

class AutoDevSettingsEditor(project: Project) : SettingsEditor<AutoDevConfiguration>() {
    private var completionProvider = DtCommandCompletionProvider()
    private var panel: JComponent? = null

    private val githubRepo = DtCommandLineEditor(project, completionProvider)
    private val storyId = DtCommandLineEditor(project, completionProvider)

    override fun createEditor(): JComponent = panel {
        row("GitHub Project (owner/repo)") {
            cell(githubRepo)
                .horizontalAlign(HorizontalAlign.FILL)
        }

        row("Story ID:") {
            cell(storyId)
                .horizontalAlign(HorizontalAlign.FILL)
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

