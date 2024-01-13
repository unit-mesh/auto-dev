package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.ui.ClickListener
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities

class AutoDevStatusBarWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
    override fun ID(): String = "AutoDev"
    override fun createInstance(project: Project): StatusBarWidget {
        return AutoDevStatusBarWidget(project)
    }

    override fun getComponent(): JComponent {
        val jLabel = JLabel()
        jLabel.icon = AutoDevIcons.DARK
        jLabel.toolTipText = AutoDevBundle.message("autodev.statusbar.toolTipText")

        object : ClickListener() {
            override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    // todo: show popup
                    return true
                }
                return true
            }
        }.installOn(jLabel)

        return jLabel
    }


    override fun createPopup(context: DataContext): ListPopup? {
        val statusGroup = DefaultActionGroup()
        statusGroup.add(AutoDevStatusItemAction())

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
