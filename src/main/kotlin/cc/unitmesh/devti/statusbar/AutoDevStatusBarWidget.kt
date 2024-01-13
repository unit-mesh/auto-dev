package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup

class AutoDevStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
    override fun ID(): String = "AutoDev"
    override fun createInstance(project: Project): StatusBarWidget {
        return AutoDevStatusBarWidget(project)
    }

    override fun createPopup(context: DataContext): ListPopup? {
//      TODO: show different status in menu
//        val currentStatus = AutoDevStatusService.currentStatus.first
        val statusGroup = DefaultActionGroup()

        val configuredGroup = ActionManager.getInstance().getAction("autodev.statusBarPopup") as? ActionGroup
            ?: return null
        statusGroup.addAll(configuredGroup)

        return JBPopupFactory.getInstance().createActionGroupPopup(
            AutoDevBundle.message("autodev.statusbar.popup.title"),
            statusGroup,
            context,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            true
        )
    }

    override fun getWidgetState(file: VirtualFile?): WidgetState {
        val widgetState = WidgetState("", "", true)
        widgetState.icon = AutoDevIcons.AI_COPILOT
        return widgetState
    }

    override fun dispose() {
        super.dispose()
    }
}
