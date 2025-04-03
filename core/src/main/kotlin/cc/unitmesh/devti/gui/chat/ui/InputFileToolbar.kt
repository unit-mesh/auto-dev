package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.chat.ui.viewmodel.FileListViewModel
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.Box
import javax.swing.JToolBar

object InputFileToolbar {
    fun createToolbar(project: Project, viewModel: FileListViewModel): JToolBar {
        val toolbar = JToolBar()
        toolbar.isFloatable = false

        val reminderLabel = JBLabel(AutoDevBundle.message("chat.panel.select.files"))
        reminderLabel.border = JBUI.Borders.empty(5)
        reminderLabel.foreground = UIUtil.getContextHelpForeground()
        toolbar.add(reminderLabel)

        toolbar.add(Box.createHorizontalGlue())

        val recentFiles = LinkLabel(AutoDevBundle.message("chat.panel.add.openFiles"), null) { _: LinkLabel<Unit>, _: Unit? ->
            viewModel.addRecentlyOpenedFiles()
        }
        
        recentFiles.mediumFontFunction()
        recentFiles.border = JBUI.Borders.emptyRight(10)
        toolbar.add(recentFiles)

        val clearAll = LinkLabel(AutoDevBundle.message("chat.panel.clear.all"), null) { _: LinkLabel<Unit>, _: Unit? ->
            viewModel.clearAllFiles()
        }

        clearAll.mediumFontFunction()
        clearAll.border = JBUI.Borders.emptyRight(20)

        toolbar.add(clearAll)

        return toolbar
    }
}
