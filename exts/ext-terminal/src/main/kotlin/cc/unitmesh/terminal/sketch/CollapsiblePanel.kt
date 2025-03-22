package cc.unitmesh.terminal.sketch

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

class CollapsiblePanel(title: String, private val contentPanel: JComponent, initiallyCollapsed: Boolean = false) :
    JBPanel<CollapsiblePanel>(BorderLayout()) {

    private var isCollapsed = initiallyCollapsed
    private val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
    val titleLabel = JBLabel(title).apply {
        border = JBUI.Borders.empty(4)
    }

    private val toggleLabel = JBLabel()

    init {
        headerPanel.add(titleLabel, BorderLayout.CENTER)
        headerPanel.add(toggleLabel, BorderLayout.EAST)
        headerPanel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        updateToggleIcon()

        headerPanel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                toggleContent()
            }
        })

        add(headerPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)

        contentPanel.isVisible = !isCollapsed
        border = JBUI.Borders.empty()
    }

    private fun updateToggleIcon() {
        toggleLabel.icon = if (isCollapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown
    }

    private fun toggleContent() {
        setCollapsed(!isCollapsed)
    }

    fun setCollapsed(collapsed: Boolean) {
        isCollapsed = collapsed
        contentPanel.isVisible = !isCollapsed
        updateToggleIcon()
        revalidate()
        repaint()
    }

    fun setCollapsed(collapsed: Boolean, content: JPanel) {
        setCollapsed(collapsed)
    }

    fun expand() {
        setCollapsed(false)
    }

    fun collapse() {
        setCollapsed(true)
    }

    fun isCollapsed(): Boolean = isCollapsed
    
    fun setTitle(title: String) {
        titleLabel.text = title
    }
}
