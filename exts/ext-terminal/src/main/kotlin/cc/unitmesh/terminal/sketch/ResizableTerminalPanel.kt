package cc.unitmesh.terminal.sketch

import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class ResizableTerminalPanel(terminalWidget: JBTerminalWidget) : JPanel(BorderLayout()) {
    private val minHeight = 48
    private val maxHeight = 600
    private var startY = 0
    private var startHeight = 0

    init {
        add(terminalWidget.component, BorderLayout.CENTER)

        val dragHandle = JPanel().apply {
            preferredSize = Dimension(Int.MAX_VALUE, 5)
            background = UIUtil.getPanelBackground()
            cursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) // Change to south resize cursor
        }

        val dragIndicator = JLabel("≡", SwingConstants.CENTER).apply {
            foreground = JBColor.GRAY
        }
        dragHandle.add(dragIndicator)

        add(dragHandle, BorderLayout.SOUTH) // Move handle to bottom of panel

        dragHandle.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                startY = e.y
                startHeight = preferredSize.height
            }
        })

        dragHandle.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val newHeight = startHeight + e.y - startY // Adjust calculation for bottom handle

                if (newHeight in minHeight..maxHeight) {
                    preferredSize = Dimension(preferredSize.width, newHeight)
                    revalidate()
                    repaint()
                }
            }
        })
    }
}