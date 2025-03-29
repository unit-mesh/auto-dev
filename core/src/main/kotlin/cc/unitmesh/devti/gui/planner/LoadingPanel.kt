package cc.unitmesh.devti.gui.planner

import com.intellij.openapi.project.Project
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.text.BadLocationException
import javax.swing.text.StyleConstants

class LoadingPanel(val project: Project) : JPanel() {
    private val loadingMessages: MutableList<String> = mutableListOf<String>(
        "🤔 Analyzing your request...",
        "💡 Generating a plan...",
        "⚙️ Processing the steps...",
        "✨ Almost there..."
    )

    private val messagePane: JTextPane
    private val progressBar: JProgressBar
    private val emojiLabel: JLabel
    private val contentPanel: JPanel
    private val glassPanel: JPanel

    private val typingTimer: Timer
    private val transitionTimer: Timer
    private var currentText = ""
    private var charIndex = 0
    private var messageIndex = 0
    private var opacity = 0.0f
    private var fadeIn = true
    private var isDarkMode = false

    private val gradientTimer: Timer
    private var gradientPosition = 0.0f
    private val gradientSpeed = 0.005f

    private val lightBackground = Color(245, 247, 250)
    private val darkBackground = Color(30, 32, 40)
    private val lightForeground = Color(50, 50, 50)
    private val darkForeground = Color(220, 220, 220)
    private val lightBorder = Color(200, 210, 230)
    private val darkBorder = Color(60, 65, 80)
    private val progressColor = Color(59, 130, 246)
    private val progressBackground = Color(229, 231, 235)
    private val darkProgressBackground = Color(55, 65, 81)

    init {
        setLayout(BorderLayout())
        setBorder(EmptyBorder(20, 20, 20, 20))
        setOpaque(false)

        glassPanel = object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

                val width = getWidth()
                val height = getHeight()


                // Create rounded rectangle for the panel
                val roundedRect: RoundRectangle2D =
                    RoundRectangle2D.Float(0f, 0f, width.toFloat(), height.toFloat(), 15f, 15f)
                g2d.setClip(roundedRect)

                val color1 = if (isDarkMode) Color(30, 40, 70, 200) else Color(240, 245, 255, 200)
                val color2 = if (isDarkMode) Color(40, 30, 70, 200) else Color(245, 240, 255, 200)
                val color3 = if (isDarkMode) Color(35, 35, 60, 200) else Color(250, 245, 255, 200)

                val pos1 = (gradientPosition) % 1.0f
                val pos2 = (gradientPosition + 0.33f) % 1.0f
                val pos3 = (gradientPosition + 0.66f) % 1.0f

                val gradient = GradientPaint(
                    width * pos1, 0f, color1,
                    width * pos2, height.toFloat(), color2
                )

                g2d.setPaint(gradient)
                g2d.fill(roundedRect)

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f))
                g2d.setColor(Color.WHITE)
                g2d.fillRect(0, 0, width, height / 2)

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity))
                g2d.dispose()
            }
        }

        glassPanel.setBorder(LineBorder(if (isDarkMode) darkBorder else lightBorder, 1, true))
        glassPanel.setOpaque(false)

        contentPanel = JPanel(BorderLayout(10, 10))
        contentPanel.setOpaque(false)
        contentPanel.setBorder(EmptyBorder(15, 15, 15, 15))

        emojiLabel = JLabel()
        emojiLabel.setFont(Font("Segoe UI Emoji", Font.PLAIN, 24))
        emojiLabel.setHorizontalAlignment(SwingConstants.CENTER)
        emojiLabel.setPreferredSize(Dimension(40, 40))

        messagePane = JTextPane()
        messagePane.setEditable(false)
        messagePane.setOpaque(false)
        messagePane.setFont(Font("Monospaced", Font.PLAIN, 14))
        messagePane.setBorder(null)

        progressBar = JProgressBar(0, 100)
        progressBar.setStringPainted(false)
        progressBar.setBorderPainted(false)
        progressBar.setOpaque(false)
        progressBar.setPreferredSize(Dimension(0, 5))
        progressBar.setUI(object : BasicProgressBarUI() {
            override fun paintDeterminate(g: Graphics, c: JComponent) {
                val g2d = g.create() as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                val width = c.getWidth()
                val height = c.getHeight()

                g2d.setColor(if (isDarkMode) darkProgressBackground else progressBackground)
                g2d.fillRoundRect(0, 0, width, height, height, height)

                val progressWidth = (width * progressBar.getPercentComplete()).toInt()
                if (progressWidth > 0) {
                    g2d.setColor(progressColor)
                    g2d.fillRoundRect(0, 0, progressWidth, height, height, height)
                }

                g2d.dispose()
            }

            override fun paintIndeterminate(g: Graphics, c: JComponent) {
                paintDeterminate(g, c)
            }
        })

        val textPanel = JPanel(BorderLayout(0, 5))
        textPanel.setOpaque(false)
        textPanel.add(messagePane, BorderLayout.CENTER)
        textPanel.add(progressBar, BorderLayout.SOUTH)

        contentPanel.add(emojiLabel, BorderLayout.WEST)
        contentPanel.add(textPanel, BorderLayout.CENTER)

        glassPanel.add(contentPanel, BorderLayout.CENTER)
        add(glassPanel, BorderLayout.CENTER)

        typingTimer = Timer(60, ActionListener { e: ActionEvent? -> updateTypingAnimation() })
        transitionTimer = Timer(20, ActionListener { e: ActionEvent? ->
            if (fadeIn) {
                opacity += 0.05f
                if (opacity >= 1.0f) {
                    opacity = 1.0f
                    fadeIn = false
                    transitionTimer.stop()
                    typingTimer.start()
                }
            } else {
                opacity -= 0.05f
                if (opacity <= 0.0f) {
                    opacity = 0.0f
                    fadeIn = true
                    transitionTimer.stop()

                    charIndex = 0
                    currentText = ""
                    messageIndex = (messageIndex + 1) % loadingMessages.size
                    updateEmojiLabel()

                    transitionTimer.start()
                }
            }
            glassPanel.repaint()
        })

        gradientTimer = Timer(50, ActionListener { e: ActionEvent? ->
            gradientPosition += gradientSpeed
            if (gradientPosition > 1.0f) {
                gradientPosition = 0.0f
            }
            glassPanel.repaint()
        })

        updateEmojiLabel()
        startAnimations()

        val darkModeToggle = JToggleButton("Dark Mode")
        darkModeToggle.addActionListener(ActionListener { e: ActionEvent? -> toggleDarkMode() })
        add(darkModeToggle, BorderLayout.SOUTH)

        updateColors()
    }

    private fun startAnimations() {
        fadeIn = true
        opacity = 0.0f
        transitionTimer.start()
        gradientTimer.start()
    }

    private fun updateTypingAnimation() {
        val message = loadingMessages.get(messageIndex)
        val textPart = message.substring(message.indexOf(' ') + 1)

        if (charIndex < textPart.length) {
            currentText += textPart.get(charIndex)
            updateMessageText()
            charIndex++

            val progress = ((charIndex.toFloat() / textPart.length) * 100).toInt()
            progressBar.setValue(progress)
        } else {
            typingTimer.stop()
            val pauseTimer = Timer(1200, ActionListener { e: ActionEvent? ->
                (e!!.getSource() as Timer).stop()
                fadeIn = false
                transitionTimer.start()
            })
            pauseTimer.setRepeats(false)
            pauseTimer.start()
        }
    }

    private fun updateMessageText() {
        val doc = messagePane.getStyledDocument()
        val style = messagePane.addStyle("MessageStyle", null)
        StyleConstants.setForeground(style, if (isDarkMode) darkForeground else lightForeground)
        StyleConstants.setFontFamily(style, "Monospaced")
        StyleConstants.setFontSize(style, 14)

        try {
            doc.remove(0, doc.getLength())
            doc.insertString(0, currentText, style)

            StyleConstants.setForeground(style, progressColor)
            doc.insertString(doc.getLength(), "|", style)
        } catch (e: BadLocationException) {
            e.printStackTrace()
        }
    }

    private fun updateEmojiLabel() {
        val message = loadingMessages.get(messageIndex)
        val emoji = message.substring(0, message.indexOf(' '))
        emojiLabel.setText(emoji)
    }

    private fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        updateColors()
        glassPanel.repaint()
    }

    private fun updateColors() {
        setBackground(if (isDarkMode) darkBackground else lightBackground)
        messagePane.setForeground(if (isDarkMode) darkForeground else lightForeground)
        glassPanel.setBorder(LineBorder(if (isDarkMode) darkBorder else lightBorder, 1, true))
        updateMessageText()
    }

    override fun removeNotify() {
        super.removeNotify()
        typingTimer.stop()
        transitionTimer.stop()
        gradientTimer.stop()
    }
}