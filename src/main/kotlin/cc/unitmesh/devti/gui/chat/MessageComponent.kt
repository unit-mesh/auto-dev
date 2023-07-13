package cc.unitmesh.devti.gui.chat


import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NotNull
import java.awt.*
import javax.accessibility.AccessibleContext
import javax.swing.*

class DisplayComponent(question: String) : JEditorPane() {
    init {
        this.contentType = "text/plain;charset=UTF-8"
        this.putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        this.font = UIUtil.getMenuFont()
        this.isEditable = false
        this.border = JBEmptyBorder(8)
        this.text = question
        this.isOpaque = false
        this.putClientProperty(
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
            StringUtil.unescapeXmlEntities(StringUtil.stripHtml(question, " "))
        )

        if (this.caret != null) {
            this.caretPosition = 0
        }
    }

    fun updateMessage(content: String) {
        this.text = content
    }
}

class MessageComponent(private val question: String, isPrompt: Boolean) : JBPanel<MessageComponent>() {
    private val component: DisplayComponent = DisplayComponent(question)
    private var answer: String? = null

    init {
        isDoubleBuffered = true
        isOpaque = true
        background = when {
            isPrompt -> JBColor(0xEAEEF7, 0x45494A)
            else -> {
                JBColor(0xE0EEF7, 0x2d2f30)
            }
        }

        this.border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        val centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))
        centerPanel.isOpaque = false
        centerPanel.border = JBUI.Borders.emptyRight(8)

        component.updateMessage(question)
        component.revalidate()
        component.repaint()
        centerPanel.add(component)

        add(centerPanel, BorderLayout.CENTER)
    }

    fun updateContent(content: String) {
        MessageWorker(content).execute()
    }

    fun updateSourceContent(source: String?) {
        answer = source
    }

    fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val bounds: Rectangle = bounds
            scrollRectToVisible(bounds)
        }
    }

    internal inner class MessageWorker(private val message: String) : SwingWorker<Void?, String?>() {
        @Throws(Exception::class)
        override fun doInBackground(): Void? {
            return null
        }

        override fun done() {
            try {
                get()
                component.updateMessage(message)
                component.updateUI()
            } catch (e: Exception) {
                logger.error(message, e.message)
            }
        }
    }

    companion object {
        private val logger = Logger.getInstance(MessageComponent::class.java)

    }
}