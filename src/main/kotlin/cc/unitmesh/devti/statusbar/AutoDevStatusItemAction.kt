package cc.unitmesh.devti.statusbar

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class AutoDevStatusItemAction : AnAction(), DumbAware {
    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.isEnabled = false
        presentation.text = "AutoDev"
    }

    override fun actionPerformed(e: AnActionEvent) {}

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
