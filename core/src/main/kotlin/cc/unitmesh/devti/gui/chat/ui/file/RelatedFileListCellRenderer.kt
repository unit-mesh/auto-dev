package cc.unitmesh.devti.gui.chat.ui.file

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class RelatedFileListCellRenderer(val project: Project) : ListCellRenderer<FilePresentation> {
    override fun getListCellRendererComponent(
        list: JList<out FilePresentation>,
        value: FilePresentation,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val fileItemPanel = value.panel ?: createFileItemPanel(value)
        val fileInfoPanel = value.namePanel
        if (isSelected) {
            fileInfoPanel?.background = list.selectionBackground
            fileInfoPanel?.foreground = list.selectionForeground
        } else {
            fileInfoPanel?.background = fileItemPanel.background
            fileInfoPanel?.foreground = list.foreground
        }

        return fileItemPanel
    }

    /**
     * Creates a panel for displaying a file item in the list.
     */
    private fun createFileItemPanel(value: FilePresentation): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            accessibleContext.accessibleName = "Element Panel"
            border = JBUI.Borders.empty(2, 5)
            val fileInfoPanel = JPanel(layout)
            
            val fileIconLabel = JLabel(value.virtualFile.fileType.icon ?: AllIcons.FileTypes.Unknown)
            fileInfoPanel.add(fileIconLabel)
            fileInfoPanel.toolTipText = value.relativePath(project)

            val fileNameLabel = JLabel(value.displayName())
            fileInfoPanel.add(fileNameLabel)
            
            add(fileInfoPanel)

            val closeButton = JLabel(AllIcons.Actions.Close)
            closeButton.border = JBUI.Borders.empty()
            add(closeButton, BorderLayout.EAST)

            value.panel = this
            value.namePanel = fileInfoPanel
            this.toolTipText = value.relativePath(project)
        }
    }
}