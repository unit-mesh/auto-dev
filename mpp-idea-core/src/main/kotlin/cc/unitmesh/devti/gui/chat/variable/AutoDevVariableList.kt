package cc.unitmesh.devti.gui.chat.variable

import cc.unitmesh.devti.custom.compile.CustomVariable
import com.intellij.ui.components.JBList
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter

class AutoDevVariableList(
    val list: List<AutoDevVariableListItemComponent>,
    val callback: ((AutoDevVariableListItemComponent) -> Unit),
) : JBList<AutoDevVariableListItemComponent>(list) {
    init {
        border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
        setCellRenderer(InputListCellRenderer())
        addMouseListener(object : MouseInputAdapter() {
            override fun mouseClicked(event: MouseEvent?) {
                val item = selectedValue ?: return
                callback.invoke(item)
            }
        })
    }

    companion object {
        fun from(all: List<CustomVariable>, function: (AutoDevVariableListItemComponent) -> Unit): AutoDevVariableList {
            val list = all.map {
                AutoDevVariableListItemComponent(it)
            }
            return AutoDevVariableList(list, function)
        }
    }
}

