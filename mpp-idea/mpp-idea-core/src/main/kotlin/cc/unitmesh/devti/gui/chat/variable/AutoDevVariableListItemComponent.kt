package cc.unitmesh.devti.gui.chat.variable

import cc.unitmesh.devti.custom.compile.CustomVariable
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel

class AutoDevVariableListItemComponent(val customVariable: CustomVariable) : JPanel(BorderLayout()) {
    init {
        add(JLabel("$${customVariable.variable}"), BorderLayout.WEST)
        val label = JLabel(customVariable.description)
        label.border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
        label.foreground = JBColor.namedColor("Component.infoForeground", JBColor(Gray.x99, Gray.x78))
        add(label, BorderLayout.EAST)
    }
}