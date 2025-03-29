package cc.unitmesh.devti.gui.planner

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.Timer
import javax.swing.border.LineBorder
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

class LoadingPanel(project: Project) : JPanel(BorderLayout()) {
    private val textPane = JTextPane()
    private val timer = Timer(50, null)
    private var currentText = ""
    private var currentIndex = 0
    private val loadingTexts = listOf(
        "🤔 Analyzing your request...",
        "💡 Generating a plan...",
        "⚙️ Processing the steps...",
        "✨ Almost there..."
    )
    private var currentTextIndex = 0

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.empty(10)
        preferredSize = Dimension(0, JBUI.scale(60))

        val containerPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val gradient = GradientPaint(
                    0f, 0f,
                    JBUI.CurrentTheme.ToolWindow.background(),
                    0f, height.toFloat(),
                    JBUI.CurrentTheme.ToolWindow.background().darker()
                )
                g2d.paint = gradient
                g2d.fillRect(0, 0, width, height)
            }
        }
        containerPanel.border = LineBorder(UIUtil.getBoundsColor(), 1, true)
        containerPanel.preferredSize = Dimension(0, JBUI.scale(50))

        textPane.apply {
            background = Color(0, 0, 0, 0)
            foreground = UIUtil.getLabelForeground()
            font = JBUI.Fonts.create("Monospaced", 14)
            isEditable = false
            isOpaque = false
            border = JBUI.Borders.empty(10, 0)
        }

        containerPanel.add(textPane, BorderLayout.CENTER)
        add(containerPanel, BorderLayout.CENTER)

        startTypingAnimation()
    }

    private fun startTypingAnimation() {
        timer.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                if (currentIndex < loadingTexts[currentTextIndex].length) {
                    currentText += loadingTexts[currentTextIndex][currentIndex]
                    updateText()
                    currentIndex++
                } else {
                    currentIndex = 0
                    currentText = ""
                    currentTextIndex = (currentTextIndex + 1) % loadingTexts.size
                }
            }
        })
        timer.start()
    }

    private fun updateText() {
        val doc = textPane.styledDocument
        val style = SimpleAttributeSet()
        StyleConstants.setForeground(style, UIUtil.getLabelForeground())
        StyleConstants.setFontSize(style, 14)
        StyleConstants.setFontFamily(style, "Monospaced")

        try {
            doc.remove(0, doc.length)
            doc.insertString(0, currentText, style)
        } catch (e: Exception) {
            // Ignore any document modification errors
        }
    }

    override fun removeNotify() {
        super.removeNotify()
        timer.stop()
    }
}