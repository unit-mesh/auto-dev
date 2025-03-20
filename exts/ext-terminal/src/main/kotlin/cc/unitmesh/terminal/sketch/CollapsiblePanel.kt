package cc.unitmesh.terminal.sketch

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

class CollapsiblePanel(title: String, content: JPanel) : JBPanel<CollapsiblePanel>(BorderLayout()) {
    private var isCollapsed = false
    private val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val titleLabel = JBLabel(title)
    private val toggleLabel = JBLabel("▼") // Unicode arrow: ▼ for expanded, ► for collapsed

    init {
        headerPanel.border = JBUI.Borders.empty(5)
        headerPanel.add(titleLabel, BorderLayout.CENTER)
        headerPanel.add(toggleLabel, BorderLayout.EAST)
        headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        headerPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                toggleContent(content)
            }
        })

        add(headerPanel, BorderLayout.NORTH)
        add(content, BorderLayout.CENTER)
    }

    private fun toggleContent(content: JPanel) {
        isCollapsed = !isCollapsed
        content.isVisible = !isCollapsed
        toggleLabel.text = if (isCollapsed) "►" else "▼"
        revalidate()
        repaint()
    }
}