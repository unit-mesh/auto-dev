package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.observer.agent.PlanList
import cc.unitmesh.devti.observer.agent.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.PlanSketch
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton

class PlanBoard(
    private val project: Project,
    private var content: String,
    private val planLists: MutableList<PlanList>
) : Disposable {
    var popup: JBPopup? = null
    var connection = ApplicationManager.getApplication().messageBus.connect(this)

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

        connection.subscribe(PlanUpdateListener.TOPIC, object : PlanUpdateListener {
            override fun onPlanUpdate(items: MutableList<PlanList>) {
                planSketch.updatePlan(items)
            }
        })
    }

    fun show() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            popup?.showInBestPositionFor(editor)
        } else {
            popup?.showInFocusCenter()
        }
    }

    override fun dispose() {
        connection.disconnect()
    }
}