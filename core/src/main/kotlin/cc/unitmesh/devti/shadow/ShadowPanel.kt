package cc.unitmesh.devti.shadow

import com.intellij.ui.Gray
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.LineBorder

class ShadowPanel(
    private val title: String,
    private val onSubmit: (String) -> Unit,
    private val onCancel: () -> Unit
) : JPanel(BorderLayout()) {
    private val textArea: JTextArea
    init {
        background = JBUI.CurrentTheme.ToolWindow.background()

        val mainPanel = JPanel(BorderLayout(8, 8)).apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(8),
                BorderFactory.createCompoundBorder(
                    LineBorder(Color(230, 230, 230), 1, true),
                    JBUI.Borders.empty(8)
                )
            )
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        val headerLabel = JLabel(title).apply {
            font = font.deriveFont(font.size + 2f)
            border = JBUI.Borders.emptyBottom(10)
        }

        textArea = JTextArea().apply {
            lineWrap = true
            wrapStyleWord = true
            border = null
            font = Font("Arial", Font.PLAIN, 16)
            rows = 10
        }

        val scrollPane = JBScrollPane(textArea).apply {
            border = null
            preferredSize = Dimension(400, 300)
        }

        val controlsPanel = JPanel(BorderLayout(10, 0)).apply {
            border = JBUI.Borders.emptyTop(10)
            background = JBUI.CurrentTheme.ToolWindow.background()
        }

        val buttonsPanel = JPanel().apply {
            layout = FlowLayout(FlowLayout.RIGHT, 8, 0)
            isOpaque = false
        }

        val submitButton = JButton("Submit").apply {
            addActionListener {
                val text = textArea.text
                if (text.isNotBlank()) {
                    onSubmit(text)
                }
            }

            background = Color(63, 81, 181)
            preferredSize = Dimension(90, 32)
            border = RoundedBorder(10, background)
        }
        
        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                onCancel()
            }

            background = Gray._240
            preferredSize = Dimension(90, 32)
            border = RoundedBorder(10, background)
        }

        buttonsPanel.add(submitButton)
        buttonsPanel.add(Box.createHorizontalStrut(5))
        buttonsPanel.add(cancelButton)

        controlsPanel.add(buttonsPanel, BorderLayout.EAST)

        mainPanel.add(headerLabel, BorderLayout.NORTH)
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(controlsPanel, BorderLayout.SOUTH)

        add(mainPanel, BorderLayout.CENTER)
    }

    fun getText(): String = textArea.text

    fun setText(text: String) {
        textArea.text = text
    }

    fun requestTextAreaFocus() {
        textArea.requestFocus()
    }

    private class RoundedBorder(private val radius: Int, private val color: Color) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = color
            g2.fillRoundRect(x, y, width, height, radius, radius)
            g2.dispose()
        }

        override fun getBorderInsets(c: Component): Insets {
            return JBUI.insets(4, 8)
        }

        override fun isBorderOpaque(): Boolean {
            return true
        }
    }
}
