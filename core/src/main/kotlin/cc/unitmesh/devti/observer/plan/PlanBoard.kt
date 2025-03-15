package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.observer.agent.PlanList
import cc.unitmesh.devti.sketch.ui.PlanSketch
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton

class PlanBoard(
    private val project: Project,
    private var content: String,
    private val planLists: MutableList<PlanList>
) {
    var popup: JBPopup? = null

    init {
        val planSketch = PlanSketch(project, content, planLists, isInPopup = true)
        popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(planSketch, null)
            .setProject(project)
            .setResizable(true)
            .setMovable(true)
            .setTitle("Thought Plan")
            .setCancelButton(MinimizeButton("Hide"))
            .setCancelCallback {
                popup?.cancel()
                true
            }
            .setFocusable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(false)
            .setCancelOnOtherWindowOpen(false)
            .createPopup()
    }


    fun show() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            popup?.showInBestPositionFor(editor)
        } else {
            popup?.showInFocusCenter()
        }
    }

}