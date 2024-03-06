package cc.unitmesh.devti.gui.chat

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import java.awt.Component
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.MouseInputAdapter

class AutoDevVariableList(
    val list: List<AutoDevVariableListComponent>,
    val callback: ((AutoDevVariableListComponent) -> Unit?)?,
) : JBList<AutoDevVariableListComponent>() {
    init {
        border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        setCellRenderer(VariableListCellRenderer())
        addMouseListener(object : MouseInputAdapter() {
            override fun mouseClicked(event: MouseEvent?) {
                val item = selectedValue ?: return
                callback?.invoke(item)
            }
        } as MouseListener)
    }
}

class VariableListCellRenderer : ListCellRenderer<AutoDevVariableListComponent> {
    private var emptyBorder: Border = BorderFactory.createEmptyBorder(1, 1, 1, 1)

    override fun getListCellRendererComponent(
        jList: JList<out AutoDevVariableListComponent>,
        value: AutoDevVariableListComponent?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        if (isSelected) {
            value!!.background = jList.selectionBackground
            value.foreground = jList.selectionForeground
        } else {
            value!!.background = jList.getBackground()
            value.foreground = jList.getForeground()
        }
        value.isEnabled = jList.isEnabled
        value.font = jList.font

        var border: Border? = null
        if (cellHasFocus) {
            if (isSelected) {
                border = UIManager.getBorder("List.focusSelectedCellHighlightBorder")
            }
            if (border == null) {
                border = UIManager.getBorder("List.focusCellHighlightBorder")
            }
        } else {
            border = this.emptyBorder
        }

        value.border = border
        return value
    }

}

class AutoDevVariableListComponent : JPanel() {
    init {
        val label = JLabel("doing something")
        label.border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        label.foreground = JBColor.namedColor("Component.infoForeground", JBColor(Gray.x99, Gray.x78))
        add(label, "East")
    }
}