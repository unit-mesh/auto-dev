package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.Box
import javax.swing.DefaultListModel
import javax.swing.JToolBar

class InputFileToolbar {
    companion object {
        /**
         * Creates a toolbar with file management functionality
         * 
         * @param project The current project
         * @param model The list model containing the file wrappers
         * @return The configured toolbar
         */
        fun createToolbar(project: Project, model: DefaultListModel<ModelWrapper>): JToolBar {
            val toolbar = JToolBar()
            toolbar.isFloatable = false

            val reminderLabel = JBLabel(AutoDevBundle.message("chat.panel.select.files"))
            reminderLabel.border = JBUI.Borders.empty(5)
            reminderLabel.foreground = UIUtil.getContextHelpForeground()
            toolbar.add(reminderLabel)

            toolbar.add(Box.createHorizontalGlue())

            val clearAll = LinkLabel(AutoDevBundle.message("chat.panel.clear.all"), null) { _: LinkLabel<Unit>, _: Unit? ->
                model.removeAllElements()
            }

            clearAll.mediumFontFunction()
            clearAll.border = JBUI.Borders.emptyRight(20)

            toolbar.add(clearAll)

            return toolbar
        }
    }
}
