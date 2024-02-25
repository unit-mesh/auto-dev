package cc.unitmesh.harmonyos.actions.auto

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoArkUiTask(
    private val project: Project,
    private val flow: AutoArkUiFlow,
    private val editor: Editor,
) : Task.Backgroundable(project, "Gen Page", true) {
    override fun run(indicator: ProgressIndicator) {
        indicator.fraction = 0.2

        indicator.text = AutoDevBundle.message("autopage.generate.clarify")
        val components = flow.clarify()

        val componentNames = components.substringAfter("[").substringBefore("]")
            .split(", ").map { it.trim() }

        indicator.fraction = 0.6
        indicator.text = AutoDevBundle.message("autopage.generate.design")
        flow.design(componentNames)

        indicator.fraction = 0.8

    }
}