package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.block.whenDisposed
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.awt.BorderLayout
import java.awt.event.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.ScrollPaneConstants


class ChatCodingComponent(private val chatCodingService: ChatCodingService, val disposable: Disposable?) :
    JBPanel<ChatCodingComponent>(),
    NullableComponent {
    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private val mainPanel = JPanel(BorderLayout(0, JBUI.scale(8)))
    private val myScrollPane = JBScrollPane(
        myList,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )
    private var inputSection: AutoDevInputSection

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

        inputSection = AutoDevInputSection(chatCodingService, chatCodingService.project, disposable, this)
        mainPanel.add(inputSection, BorderLayout.SOUTH)

        disposable?.whenDisposed(disposable) {
            myList.removeAll()
        }
    }

    fun addMessage(message: String, isMe: Boolean = false, displayPrompt: String = "") {
        val role = if (isMe) ChatRole.User else ChatRole.Assistant
        val displayText = displayPrompt.ifEmpty { message }

        val messageView = MessageView(message, role, displayText)

        myList.add(messageView)
        updateLayout()
        scrollToBottom()
        progressBar.isIndeterminate = true
        updateUI()
    }

    private fun updateLayout() {
        val layout = myList.layout
        val componentCount = myList.componentCount
        for (i in 0 until componentCount) {
            layout.removeLayoutComponent(myList.getComponent(i))
            layout.addLayoutComponent(null, myList.getComponent(i))
        }
    }

    suspend fun updateMessage(content: Flow<String>): String {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        val result = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()

        return result
    }

    private fun scrollToBottom() {
        val verticalScrollBar = myScrollPane.verticalScrollBar
        verticalScrollBar.value = verticalScrollBar.maximum
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    suspend fun updateReplaceableContent(content: Flow<String>, replaceSelectedText: (text: String) -> Unit) {
        myList.remove(myList.componentCount - 1)
        val text = updateMessageInUi(content)

        val jButton = JButton(AutoDevBundle.message("devti.chat.replaceSelection"))
        val listener = ActionListener {
            replaceSelectedText(text)
            myList.remove(myList.componentCount - 1)
        }
        jButton.addActionListener(listener)
        myList.add(jButton)

        progressBar.isIndeterminate = false
        progressBar.isVisible = false
        updateUI()
    }

    private suspend fun updateMessageInUi(content: Flow<String>): String {
        val messageView = MessageView("", ChatRole.Assistant, "")
        myList.add(messageView)

        var text = ""
        content.collect {
            text += it
            messageView.updateSourceContent(text)
            messageView.updateContent(text)
            messageView.scrollToBottom()
        }

        messageView.doneContent()

        return text
    }

//    private fun addQuestionArea() {
//        val actionPanel = JPanel(BorderLayout())
//
//        val searchTextArea = AutoDevInputField(
//            chatCodingService.project,
//            listOf()
//        )
//
//        this.inputField = searchTextArea
//
//        val listener: (ActionEvent) -> Unit = {
//            val prompt = searchTextArea.text
//            searchTextArea.text = ""
//            val context = ChatContext(null, "", "")
//
//            chatCodingService.actionType = ChatActionType.CHAT
//            chatCodingService.handlePromptAndResponse(this, object : ContextPrompter() {
//                override fun displayPrompt() = prompt
//                override fun requestPrompt() = prompt
//            }, context)
//        }
//
//        searchTextArea.addListener(object : AutoDevInputListener {
//            override fun onSubmit(component: AutoDevInputField, trigger: AutoDevInputTrigger) {
//                listener.invoke(ActionEvent(component, 0, trigger.name))
//            }
//        })
//
//        actionPanel.add(searchTextArea, BorderLayout.CENTER)
//
//        val actionButtons = JPanel(BorderLayout())
//
//        val button = JButton(AutoDevBundle.message("devti.chat.send"))
//        button.addActionListener(listener)
//
//        actionButtons.add(button, BorderLayout.NORTH)
//        actionPanel.add(actionButtons, BorderLayout.EAST)
//
//        mainPanel.add(actionPanel, BorderLayout.SOUTH)
//    }

    fun setContent(trimMargin: String) {
        inputSection.setContent(trimMargin)
    }
}