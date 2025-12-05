package cc.unitmesh.ide.javascript.flow

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class AutoPageTask(
    private val project: Project,
    private val flow: AutoPageFlow,
    private val editor: Editor,
    private val autoPage: ReactAutoPage
) : Task.Backgroundable(project, "Gen Page", true) {
    override fun run(indicator: ProgressIndicator) {
        indicator.fraction = 0.2

        indicator.text = AutoDevBundle.message("autopage.generate.clarify")
        val components = flow.clarify()
        // tables will be list in string format, like: `[table1, table2]`, we need to parse to Lists
        val componentNames = components.substringAfter("[").substringBefore("]")
            .split(", ").map { it.trim() }

        val filterComponents = autoPage.filterComponents(componentNames)
        flow.context.components = filterComponents.map { it.format() }

        indicator.fraction = 0.6
        indicator.text = AutoDevBundle.message("autopage.generate.design")
        flow.design(filterComponents)

        indicator.fraction = 0.8
    }
}