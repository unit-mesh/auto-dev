package cc.unitmesh.devti.statusbar

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.ui.ClickListener
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingUtilities

class AutoDevStatusBarWidget(val project: Project) : CustomStatusBarWidget {
    override fun ID(): String = "AutoDev"

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
}
