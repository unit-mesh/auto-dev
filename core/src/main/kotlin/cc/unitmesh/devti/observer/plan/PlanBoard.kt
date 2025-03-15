package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.observer.agent.PlanList
import cc.unitmesh.devti.observer.agent.PlanUpdateListener
import cc.unitmesh.devti.sketch.ui.PlanSketch
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton

@Service(Service.Level.PROJECT)
class PlanBoard(private val project: Project) : Disposable {
    var popup: JBPopup? = null
    var connection = ApplicationManager.getApplication().messageBus.connect(this)
    var planSketch: PlanSketch = PlanSketch(project, "", mutableListOf())

    init {
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

    fun show(content: String, planLists: MutableList<PlanList>) {
        planSketch.updatePlan(planLists)
        if (popup?.isVisible == true) return

        try {
            popup?.showInFocusCenter()
        } catch (e: Exception) {
            logger<PlanBoard>().error("Failed to show popup", e)
        }
    }

    override fun dispose() {
        connection.disconnect()
    }
}