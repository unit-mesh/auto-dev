package cc.unitmesh.devti.gui.chat


import cc.unitmesh.devti.gui.chat.block.*
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import kotlin.jvm.internal.Ref

class MessageView(private val message: String, role: ChatRole) : JBPanel<MessageView>(), DataProvider {
    private val component: DisplayComponent = DisplayComponent(message)
    private var answer: String? = null

    init {
        isDoubleBuffered = true
        isOpaque = true
        background = when (role) {
            ChatRole.System -> JBColor(0xEAEEF7, 0x45494A)
            ChatRole.Assistant -> JBColor(0xE0EEF7, 0x2d2f30)
            ChatRole.User -> JBColor(0xE0EEF7, 0x2d2f30)
        }

        this.border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        val centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))
        centerPanel.isOpaque = false
        centerPanel.border = JBUI.Borders.emptyRight(8)

        component.updateMessage(message)
        component.revalidate()
        component.repaint()
        centerPanel.add(component)

        add(centerPanel, BorderLayout.CENTER)

//        layoutAll(message)
    }

    fun layoutAll(messageText: String): List<MessageBlock> {
        var currentContextType = MessageBlockType.PlainText
        val blockStart: Ref.IntRef = Ref.IntRef()

        val parts = ArrayList<MessageBlock>()

        for ((index, item) in messageText.withIndex()) {
            val param = Parameters(item, index, messageText)
            val suggestTypeChange =
                MessageCodeBlockCharProcessor().suggestTypeChange(param, currentContextType, blockStart.element)

            if (suggestTypeChange != null) {
                when {
                    suggestTypeChange.contextType == currentContextType -> {
                        if (suggestTypeChange.borderType == BorderType.START) {
                            logger.error("suggestTypeChange return $currentContextType START while there is already $currentContextType opened")
                        } else {
                            pushPart(blockStart, messageText, currentContextType, message, parts, index)
                        }
                    }

                    suggestTypeChange.borderType == BorderType.START -> {
                        if (index > blockStart.element) {
                            pushPart(blockStart, messageText, currentContextType, message, parts, index - 1)
                        }
                        blockStart.element = index
                        currentContextType = suggestTypeChange.contextType
                    }

                    else -> {
                        logger.error("suggestTypeChange return $currentContextType END when there wasn't open tag")
                    }
                }
            }
        }

        if (blockStart.element < messageText.length) {
            pushPart(blockStart, messageText, currentContextType, message, parts, messageText.length - 1)
        }

        return parts
    }

    private fun pushPart(
        blockStart: Ref.IntRef,
        messageText: String,
        currentContextType: MessageBlockType,
        message: String,
        list: List<MessageBlock>,
        partUpperOffset: Int
    ) {

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
        private val logger = logger<MessageView>()

    }

    override fun getData(dataId: String): Any? {
        TODO("Not yet implemented")
    }
}