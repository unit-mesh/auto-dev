package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class RelatedFileListCellRenderer : ListCellRenderer<ModelWrapper> {
    override fun getListCellRendererComponent(
        list: JList<out ModelWrapper>,
        value: ModelWrapper,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val panel = value.panel ?: JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)).apply {
            accessibleContext.accessibleName = "Element Panel"

            border = JBUI.Borders.empty(2, 5)

            val namePanel = JPanel(layout)
            val iconLabel = JLabel(value.virtualFile.fileType.icon ?: AllIcons.FileTypes.Unknown)
            namePanel.add(iconLabel)

            val nameLabel = JLabel(value.virtualFile.name)
            namePanel.add(nameLabel)

            add(namePanel)
            val closeLabel = JLabel(AllIcons.Actions.Close)
            closeLabel.border = JBUI.Borders.empty()
            add(closeLabel, BorderLayout.EAST)

            value.panel = this
            value.namePanel = namePanel
        }
        val namePanel = value.namePanel
        if (isSelected) {
            namePanel?.background = list.selectionBackground
            namePanel?.foreground = list.selectionForeground
        } else {
            namePanel?.background = list.background
            namePanel?.foreground = list.foreground
        }

        return panel
    }
}