package cc.unitmesh.devti.startup.third

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

class ShireSonarLintToolWindowListener : ToolWindowManagerListener {
    override fun toolWindowShown(toolWindow: ToolWindow) {
        if (toolWindow.id != "SonarLint") return

        val action = ActionManager.getInstance().getAction("ShireSonarLintAction")

        val contentManager = toolWindow.contentManager
        val content = contentManager.getContent(0) ?: return

        val simpleToolWindowPanel = content.component as? SimpleToolWindowPanel
        val actionToolbar = simpleToolWindowPanel?.toolbar?.components?.get(0) as? ActionToolbar ?: return
        val actionGroup = actionToolbar.actionGroup as? DefaultActionGroup

        if (actionGroup?.containsAction(action) == false) {
            actionGroup.add(action)
        }
    }
}