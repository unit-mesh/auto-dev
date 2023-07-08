package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.openapi.ui.NullableComponent
import com.intellij.ui.EditorTextField
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.event.*
import javax.swing.*

class ChatCodingComponent(private val chatCodingService: ChatCodingService) : JBPanel<ChatCodingComponent>(),
    NullableComponent {
    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private val mainPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
    private val myScrollPane = JBScrollPane(
        myList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )

    init {
        val splitter = OnePixelSplitter(true, .98f)
        splitter.dividerWidth = 2

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        layout = BorderLayout(JBUI.scale(7), 0)
        background = UIUtil.getListBackground()
        mainPanel.isOpaque = false
        add(mainPanel, BorderLayout.CENTER)

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()
        myScrollPane.border = JBEmptyBorder(10, 15, 10, 15)

        splitter.firstComponent = myScrollPane

        progressBar = JProgressBar()
        splitter.secondComponent = progressBar
        mainPanel.add(splitter)
        myScrollPane.verticalScrollBar.autoscrolls = true
        addQuestionArea()
    }

    fun add(message: String, isMe: Boolean = false) {
        val messageComponent = MessageComponent(message, isMe)
        val jbScrollPane = JBScrollPane(
            messageComponent, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        )

        myList.add(jbScrollPane)
        progressBar.isIndeterminate = true
        updateUI()
    }

    fun updateMessage(message: String) {
        myList.remove(myList.componentCount - 1)
        val messageComponent = MessageComponent(message, false)
        myList.add(messageComponent)
        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    fun updateReplaceableContent(content: String, replaceSelectedText: () -> Unit) {
        myList.remove(myList.componentCount - 1)
        val messageComponent = MessageComponent(content, false)

        val jButton = JButton(AutoDevBundle.message("devti.chat.replaceSelection"))
        val listener = ActionListener {
            replaceSelectedText()
            myList.remove(myList.componentCount - 1)
        }

        jButton.addActionListener(listener)
        myList.add(messageComponent)
        myList.add(jButton)

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    private fun addQuestionArea() {
        val actionPanel = JPanel(BorderLayout())

        val searchTextArea = EditorTextField()
        searchTextArea.setOneLineMode(false)

        val listener: (ActionEvent) -> Unit = {
            val prompt = searchTextArea.text
            searchTextArea.text = ""
            chatCodingService.actionType = ChatBotActionType.REFACTOR
            chatCodingService.handlePromptAndResponse(this, object : PromptFormatter {
                override fun getUIPrompt() = prompt
                override fun getRequestPrompt() = prompt
            })
        }

        searchTextArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    listener.invoke(ActionEvent(e.source, e.id, e.paramString()))
                }
            }
        })

        actionPanel.add(searchTextArea, BorderLayout.CENTER)

        val actionButtons = JPanel(BorderLayout())
        val clearChat = LinkLabel<String>(AutoDevBundle.message("devti.chat.clear"), null)
        clearChat.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                myList.removeAll()
                updateUI()
            }
        })

        clearChat.border = JBEmptyBorder(5, 5, 5, 5)

        val button = JButton(AutoDevBundle.message("devti.chat.send"))
        button.addActionListener(listener)

        actionButtons.add(button, BorderLayout.NORTH)
        actionButtons.add(clearChat, BorderLayout.SOUTH)
        actionPanel.add(actionButtons, BorderLayout.EAST)

        mainPanel.add(actionPanel, BorderLayout.SOUTH)
    }
}