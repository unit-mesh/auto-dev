package cc.unitmesh.terminal.sketch

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

class CollapsiblePanel(title: String, content: JPanel, initiallyCollapsed: Boolean = false) :
    JBPanel<CollapsiblePanel>(BorderLayout()) {

    private var isCollapsed = initiallyCollapsed
    private val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val titleLabel = JBLabel(title)
    private val toggleLabel = JBLabel()

    init {
        headerPanel.border = JBUI.Borders.empty(5)
        headerPanel.add(titleLabel, BorderLayout.CENTER)
        headerPanel.add(toggleLabel, BorderLayout.EAST)
        headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        updateToggleIcon()

        headerPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                toggleContent(content)
            }
        })

        add(headerPanel, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)

        content.isVisible = !isCollapsed
    }

    private fun updateToggleIcon() {
        toggleLabel.icon = if (isCollapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
    }

    private fun toggleContent(content: JPanel) {
        setCollapsed(!isCollapsed, content)
    }

    fun setCollapsed(collapsed: Boolean, content: JPanel) {
        isCollapsed = collapsed
        content.isVisible = !isCollapsed
        updateToggleIcon()
        revalidate()
        repaint()
    }

    fun isCollapsed(): Boolean = isCollapsed
}
