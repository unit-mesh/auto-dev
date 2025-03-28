package cc.unitmesh.devti.observer.plan

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.AutoDevPlannerToolWindow
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class CreateIssueAction : AnAction(AutoDevBundle.message("sketch.plan.create")) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        AutoDevPlannerToolWindow.showIssueInput(project) { newPlan ->
            AutoDevToolWindowFactory.Companion.sendToSketchToolWindow(project, ChatActionType.SKETCH) { ui, _ ->
                ui.sendInput(newPlan)
            }
        }
    }
}
