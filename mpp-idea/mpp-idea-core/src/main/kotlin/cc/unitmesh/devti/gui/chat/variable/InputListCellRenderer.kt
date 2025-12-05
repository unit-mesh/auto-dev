package cc.unitmesh.devti.gui.chat.variable

import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.UIManager
import javax.swing.border.Border

class InputListCellRenderer : ListCellRenderer<AutoDevVariableListItemComponent> {
    private var emptyBorder: Border = BorderFactory.createEmptyBorder(1, 1, 1, 1)

    override fun getListCellRendererComponent(
        jList: JList<out AutoDevVariableListItemComponent>,
        value: AutoDevVariableListItemComponent?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        value!!.background = if (isSelected) jList.selectionBackground else jList.background
        value.foreground = if (isSelected) jList.selectionForeground else jList.foreground
        value.isEnabled = jList.isEnabled
        value.font = jList.font
        value.border = if (cellHasFocus) {
            if (isSelected) {
                UIManager.getBorder("List.focusSelectedCellHighlightBorder")
            } else {
                UIManager.getBorder("List.focusCellHighlightBorder")
            }
        } else {
            emptyBorder
        }

        return value
    }
}