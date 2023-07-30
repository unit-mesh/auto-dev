package cc.unitmesh.devti.gui.chat


import cc.unitmesh.devti.gui.chat.block.*
import cc.unitmesh.devti.prompting.code.TestStack
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import kotlin.jvm.internal.Ref

class MessageView(private val message: String, role: ChatRole) : JBPanel<MessageView>() {
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

        add(centerPanel, BorderLayout.CENTER)

        if (role == ChatRole.User) {
            val parts = layoutAll(message, SimpleMessage(message, message, role))
            parts.forEach {
                val blockView = when(it) {
                    is CodeBlock -> {
                        val project = ProjectManager.getInstance().openProjects.firstOrNull()
                        CodeBlockView(it, project!!) { }
                    }
                    else -> TextBlockView(it)
                }
                blockView.initialize();
                blockView.getComponent()?.setForeground(JBUI.CurrentTheme.Label.foreground())
                centerPanel.add(blockView.getComponent())
            }
        } else {
            component.updateMessage(message)
            component.revalidate()
            component.repaint()
            centerPanel.add(component)
        }
    }

    fun layoutAll(messageText: String, message: CompletableMessage): List<MessageBlock> {
        val currentContextTypeRef = Ref.ObjectRef<MessageBlockType>()
        currentContextTypeRef.element = MessageBlockType.PlainText

        val blockStart: Ref.IntRef = Ref.IntRef()

        val parts = mutableListOf<MessageBlock>()

        for ((index, item) in messageText.withIndex()) {
            val param = Parameters(item, index, messageText)
            val suggestTypeChange =
                MessageCodeBlockCharProcessor().suggestTypeChange(
                    param,
                    currentContextTypeRef.element,
                    blockStart.element
                )
                    ?: continue

            when {
                suggestTypeChange.contextType == currentContextTypeRef.element -> {
                    if (suggestTypeChange.borderType == BorderType.START) {
                        logger.error("suggestTypeChange return ${currentContextTypeRef.element} START while there is already ${currentContextTypeRef.element} opened")
                    } else {
                        pushPart(blockStart, messageText, currentContextTypeRef, message, parts, index)
                    }
                }

                suggestTypeChange.borderType == BorderType.START -> {
                    if (index > blockStart.element) {
                        pushPart(blockStart, messageText, currentContextTypeRef, message, parts, index - 1)
                    }
                    blockStart.element = index
                    currentContextTypeRef.element = suggestTypeChange.contextType
                }

                else -> {
                    logger.error("suggestTypeChange return ${currentContextTypeRef.element} END when there wasn't open tag")
                }
            }
        }

        if (blockStart.element < messageText.length) {
            pushPart(blockStart, messageText, currentContextTypeRef, message, parts, messageText.length - 1)
        }

        return parts
    }

    private fun pushPart(
        blockStart: Ref.IntRef,
        messageText: String,
        currentContextType: Ref.ObjectRef<MessageBlockType>,
        message: CompletableMessage,
        list: MutableList<MessageBlock>,
        partUpperOffset: Int
    ) {
        val newPart = createPart(blockStart.element, partUpperOffset, messageText, currentContextType, message)
        list.add(newPart)

        blockStart.element = partUpperOffset + 1
        currentContextType.element = MessageBlockType.PlainText
    }

    private fun createPart(
        blockStart: Int,
        partUpperOffset: Int,
        messageText: String,
        currentContextType: Ref.ObjectRef<MessageBlockType>,
        message: CompletableMessage
    ): MessageBlock {
        check(blockStart < messageText.length)
        check(partUpperOffset < messageText.length)

        val blockText = messageText.substring(blockStart, partUpperOffset + 1)
        val part: MessageBlock = when (currentContextType.element!!) {
            MessageBlockType.CodeEditor -> CodeBlock(message)
            MessageBlockType.PlainText -> TextBlock(message)
        }

        if (blockText.isNotEmpty()) {
            part.addContent(blockText)
        }

        return part
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
}