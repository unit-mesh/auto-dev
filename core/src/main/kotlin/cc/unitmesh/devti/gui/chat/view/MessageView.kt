package cc.unitmesh.devti.gui.chat.view


import cc.unitmesh.devti.gui.chat.message.ChatRole
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.temporary.gui.block.*
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.*
import javax.swing.*
import kotlin.jvm.internal.Ref

class MessageView(val message: String, val role: ChatRole, private val displayText: String) :
    JBPanel<MessageView>(), DataProvider {
    private val myNameLabel: Component
    private var centerPanel: JPanel = JPanel(VerticalLayout(JBUI.scale(8)))
    private var messageView: SimpleMessage? = null
    private var blocks: MutableList<MessageBlock> = ArrayList()

    init {
        isDoubleBuffered = true
        isOpaque = true
        background = when (role) {
            ChatRole.System -> JBColor(0xEAEEF7, 0x45494A)
            ChatRole.Assistant -> JBColor(0xEAEEF7, 0x2d2f30)
            ChatRole.User -> JBColor(0xE0EEF7, 0x2d2f30)
        }

        val authorLabel = JLabel()
        authorLabel.setFont(JBFont.h4())
        authorLabel.setText(when (role) {
            ChatRole.System -> "System"
            ChatRole.Assistant -> "Assistant"
            ChatRole.User -> "User"
        })
        myNameLabel = authorLabel

        this.border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        centerPanel = JPanel(VerticalLayout(JBUI.scale(8)))
        centerPanel.isOpaque = false

        centerPanel.add(myNameLabel)
        add(centerPanel, BorderLayout.CENTER)

        ApplicationManager.getApplication().invokeLater {
            val simpleMessage = SimpleMessage(displayText, message, role)
            messageView = simpleMessage
            renderInPartView(simpleMessage)
        }
    }

    private fun createTitlePanel(): JPanel {
        val panel = BorderLayoutPanel()
        panel.setOpaque(false)
        panel.addToCenter(this.myNameLabel)

        val group = ActionUtil.getActionGroup("AutoDev.ToolWindow.Message.Toolbar.Assistant")

        if (group != null) {
            val toolbar = ActionToolbarImpl(javaClass.getName(), group, true)
            toolbar.component.setOpaque(false)
            toolbar.component.setBorder(JBUI.Borders.empty())
            toolbar.targetComponent = this
            panel.addToRight(toolbar.component)
        }

        panel.setOpaque(false)
        return panel
    }

    private fun renderInPartView(message: SimpleMessage) {
        val incrBlocks = layoutAll(message)
        if (incrBlocks.isEmpty()) {
            return
        }
        if (this.blocks.isEmpty()) {
            blocks.addAll(incrBlocks)
        } else {
            var newBlock: MessageBlock
            var i = 0
            while (i < minOf(this.blocks.size, incrBlocks.size)) {
                val oldBlock = this.blocks[i]
                newBlock = incrBlocks[i]
                if (oldBlock.getIdentifier() == newBlock.getIdentifier()) { // 如果新旧内容一样忽略
                    i++
                    continue
                }
                oldBlock.setNeedUpdate(true)
                oldBlock.addContent(newBlock.getTextContent())
                i++
            }
            for (j in i until incrBlocks.size) {
                newBlock = incrBlocks[j]
                newBlock.setNeedUpdate(true)
                this.blocks.add(newBlock) // 添加到 blocks
            }
        }
        renderComponent();
    }

    private fun renderComponent(){
        blocks.forEach { block ->
            if (!block.isNeedUpdate()) return@forEach

            if (block.getMessageBlockView() == null) {
                val blockView = when (block) {
                    is CodeBlock -> {
                        val project = ProjectManager.getInstance().openProjects.first()
                        CodeBlockView(block, project) { }
                    }
                    else -> TextBlockView(block)
                }
                blockView.initialize()
                block.setMessageBlockView(blockView)

                val component = blockView.getComponent() ?: return@forEach
                component.setForeground(JBUI.CurrentTheme.Label.foreground())
                centerPanel.add(component)
            }
            block.setNeedUpdate(false)
        }

    }
    private var answer: String = ""

    fun updateContent(content: String) {
        this.answer = content
        MessageWorker(content).execute()
    }

    fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val bounds: Rectangle = bounds
            scrollRectToVisible(bounds)
        }
    }

    fun reRenderAssistantOutput() {
        ApplicationManager.getApplication().invokeLater {

            centerPanel.updateUI()

            centerPanel.add(myNameLabel)
            centerPanel.add(createTitlePanel())

            val message = SimpleMessage(answer, answer, ChatRole.Assistant)
            this.messageView = message
            renderInPartView(message)

            centerPanel.revalidate()
            centerPanel.repaint()
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
                val simpleMessage = SimpleMessage(message, this@MessageView.message, role)
                renderInPartView(simpleMessage)
            } catch (e: Exception) {
                logger.error(message, e.message)
            }
        }
    }

    companion object {
        private val logger = logger<MessageView>()
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

        fun layoutAll(message: CompletableMessage): List<MessageBlock> {
            val messageText: String = message.displayText
            val contextTypeRef = Ref.ObjectRef<MessageBlockType>()
            contextTypeRef.element = MessageBlockType.PlainText

            val blockStart: Ref.IntRef = Ref.IntRef()

            val parts = mutableListOf<MessageBlock>()

            for ((index, item) in messageText.withIndex()) {
                val param = Parameters(item, index, messageText)
                val processor = MessageCodeBlockCharProcessor()
                val suggestTypeChange =
                    processor.suggestTypeChange(param, contextTypeRef.element, blockStart.element) ?: continue

                when {
                    suggestTypeChange.contextType == contextTypeRef.element -> {
                        if (suggestTypeChange.borderType == BorderType.START) {
                            logger.error("suggestTypeChange return ${contextTypeRef.element} START while there is already ${contextTypeRef.element} opened")
                        } else {
                            pushPart(blockStart, messageText, contextTypeRef, message, parts, index)
                        }
                    }

                    suggestTypeChange.borderType == BorderType.START -> {
                        if (index > blockStart.element) {
                            pushPart(blockStart, messageText, contextTypeRef, message, parts, index - 1)
                        }

                        blockStart.element = index
                        contextTypeRef.element = suggestTypeChange.contextType
                    }

                    else -> {
                        logger.error("suggestTypeChange return ${contextTypeRef.element} END when there wasn't open tag")
                    }
                }
            }

            if (blockStart.element < messageText.length) {
                pushPart(blockStart, messageText, contextTypeRef, message, parts, messageText.length - 1)
            }

            return parts
        }

    }

    override fun getData(dataId: String): CompletableMessage? {
        return if (CompletableMessage.key.`is`(dataId)) {
            return this.messageView
        } else {
            null
        }
    }
}
