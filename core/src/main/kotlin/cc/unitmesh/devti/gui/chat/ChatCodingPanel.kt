package cc.unitmesh.devti.gui.chat

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.*
import cc.unitmesh.devti.agent.model.CustomAgentConfig
import cc.unitmesh.devti.agent.view.WebBlock
import cc.unitmesh.devti.agent.view.WebBlockView
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputListener
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputTrigger
import cc.unitmesh.devti.gui.chat.view.FrontendCodeView
import cc.unitmesh.devti.gui.chat.view.MessageView
import cc.unitmesh.devti.gui.chat.welcome.WelcomePanel
import cc.unitmesh.devti.provider.ContextPrompter
import cc.unitmesh.devti.provider.devins.LanguagePromptProcessor
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.LanguageChangedCallback.componentStateChanged
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.temporary.gui.block.CodeBlock
import com.intellij.temporary.gui.block.CodeBlockView
import com.intellij.temporary.gui.block.SimpleMessage
import com.intellij.temporary.gui.block.whenDisposed
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class ChatCodingPanel(private val chatCodingService: ChatCodingService, val disposable: Disposable?) :
    SimpleToolWindowPanel(true, true),
    NullableComponent {
    private var progressBar: JProgressBar
    private val myTitle = JBLabel("Conversation")
    private val myList = JPanel(VerticalLayout(JBUI.scale(10)))
    private var inputSection: AutoDevInputSection
    private val focusMouseListener: MouseAdapter
    private var panelContent: DialogPanel
    private val myScrollPane: JBScrollPane
    private val delaySeconds: String get() = AutoDevSettingsState.getInstance().delaySeconds

    private var suggestionPanel: JPanel = JPanel(BorderLayout())

    init {
        focusMouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                focusInput()
            }
        }

        myList.add(WelcomePanel())

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()

        myScrollPane = JBScrollPane(
            myList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        myScrollPane.verticalScrollBar.autoscrolls = true
        myScrollPane.background = UIUtil.getListBackground()

        progressBar = JProgressBar()

        val actionLink = panel {
            row {
                text("").apply {
                    componentStateChanged("label.submit.issue", this.component) { c, d -> c.text = d }
                }
            }
        }

        inputSection = AutoDevInputSection(chatCodingService.project, disposable)
        inputSection.addListener(object : AutoDevInputListener {
            override fun onStop(component: AutoDevInputSection) {
                chatCodingService.stop()
                hiddenProgressBar()
            }

            override fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {
                var prompt = component.text
                component.text = ""


                if (prompt.isEmpty() || prompt.isBlank()) {
                    component.showTooltip(AutoDevBundle.message("chat.input.tips"))
                    return
                }

                val context = ChatContext(null, "", "")

                val postProcessors = LanguagePromptProcessor.instance("DevIn").firstOrNull()
                if (postProcessors != null) {
                    prompt = postProcessors.compile(chatCodingService.project, prompt)
                }

                chatCodingService.actionType = ChatActionType.CHAT
                chatCodingService.handlePromptAndResponse(this@ChatCodingPanel, object : ContextPrompter() {
                    override fun displayPrompt() = prompt
                    override fun requestPrompt() = prompt
                }, context, false)
            }
        })

        panelContent = panel {
            row { cell(myScrollPane).fullWidth().fullHeight() }.resizableRow()
            row { cell(suggestionPanel).fullWidth() }
            row { cell(progressBar).fullWidth() }
            row { cell(actionLink).alignRight() }
            row {
                border = JBUI.Borders.empty(8)
                cell(inputSection).fullWidth()
            }
        }

        setContent(panelContent)

        disposable?.whenDisposed(disposable) {
            myList.removeAll()
        }
    }

    fun focusInput() {
        val focusManager = IdeFocusManager.getInstance(chatCodingService.project)
        focusManager.doWhenFocusSettlesDown {
            focusManager.requestFocus(this.inputSection.focusableComponent, true)
        }
    }

    /**
     * Add a message to the chat panel and update ui
     */
    fun addMessage(message: String, isMe: Boolean = false, displayPrompt: String = ""): MessageView {
        val role = if (isMe) ChatRole.User else ChatRole.Assistant
        val displayText = displayPrompt.ifEmpty { message }

        val messageView = MessageView(message, role, displayText)

        myList.add(messageView)
        updateLayout()
        scrollToBottom()
        progressBar.isIndeterminate = true
        updateUI()
        return messageView
    }

    private fun updateLayout() {
        val layout = myList.layout
        for (i in 0 until myList.componentCount) {
            layout.removeLayoutComponent(myList.getComponent(i))
            layout.addLayoutComponent(null, myList.getComponent(i))
        }
    }

    fun getHistoryMessages(): List<LlmMsg.ChatMessage> {
        val messages = mutableListOf<LlmMsg.ChatMessage>()
        for (i in 0 until myList.componentCount) {
            val component = myList.getComponent(i)
            if (component is MessageView) {
                val role = LlmMsg.ChatRole.valueOf(component.role.name)
                messages.add(LlmMsg.ChatMessage(role, component.message, null))
            }
        }
        return messages
    }

    suspend fun updateMessage(content: Flow<String>): String {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        showProgressBar()

        val result = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        hiddenProgressBar()
        updateUI()

        return result
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            val verticalScrollBar = myScrollPane.verticalScrollBar
            verticalScrollBar.value = verticalScrollBar.maximum
        }
    }

    override fun isNull(): Boolean {
        return !isVisible
    }

    /**
     * Updates the replaceable content in the UI using the provided `Flow<String>`.
     *
     * @param content The flow of strings to update the UI with.
     * @param postAction A function that is called when the "Replace Selection" button is clicked,
     *                            passing the current text to be replaced in the editor.
     */
    suspend fun updateReplaceableContent(content: Flow<String>, postAction: (text: String) -> Unit) {
        myList.remove(myList.componentCount - 1)
        showProgressBar()
        val text = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        hiddenProgressBar()
        updateUI()

        postAction(text)
    }

    suspend fun updateMessageInUi(content: Flow<String>): String {
        val messageView = MessageView("", ChatRole.Assistant, "")
        myList.add(messageView)
        val startTime = System.currentTimeMillis()
        var text = ""
        val batchSize = 5
        val buffer = mutableListOf<String>()

        content
            .buffer(capacity = 10)
            .collect { newText ->
                buffer.add(newText)
                if (buffer.size >= batchSize) {
                    text += buffer.joinToString("")
                    buffer.clear()
                    messageView.updateContent(text)
                }
            }
        // 处理剩余的缓冲内容
        if (buffer.isNotEmpty()) {
            text += buffer.joinToString("")
            messageView.updateContent(text)
        }

        if (delaySeconds.isNotEmpty()) {
            val elapsedTime = System.currentTimeMillis() - startTime
            withContext(Dispatchers.IO) {
                val delaySec = delaySeconds.toLong()
                val remainingTime = maxOf(delaySec * 1000 - elapsedTime, 0)
                delay(remainingTime)
            }
        }

        return text
    }

    fun setInput(trimMargin: String) {
        inputSection.text = trimMargin
        this.focusInput()
    }

    /**
     * Resets the chat session by clearing the current session and updating the UI.
     */
    fun resetChatSession() {
        chatCodingService.stop()
        suggestionPanel.removeAll()
        chatCodingService.clearSession()
        myList.removeAll()
        myList.add(WelcomePanel())
        this.hiddenProgressBar()
        this.resetAgent()
        updateUI()
    }

    fun resetAgent() {
        inputSection.resetAgent()
    }

    fun hasSelectedCustomAgent(): Boolean {
        return inputSection.hasSelectedAgent()
    }

    fun getSelectedCustomAgent(): CustomAgentConfig {
        return inputSection.getSelectedAgent()
    }

    fun hiddenProgressBar() {
        progressBar.isVisible = false
        inputSection.showSendButton()
    }

    fun showProgressBar() {
        progressBar.isVisible = true
        inputSection.showStopButton()
    }

    fun removeLastMessage() {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        updateUI()
    }

    fun selectAgent(config: CustomAgentConfig) {
        inputSection.selectAgent(config)
    }

    fun appendWebView(content: String, project: Project) {
        val msg = SimpleMessage(content, content, ChatRole.System)
        val webBlock = WebBlock(msg)
        val blockView = WebBlockView(webBlock, project)
        val codeView = CodeBlockView(CodeBlock(msg, language = HTMLLanguage.INSTANCE), project, {})

        myList.add(FrontendCodeView(blockView, codeView))

        updateUI()
    }

    fun moveCursorToStart() {
        inputSection.moveCursorToStart()
    }

    fun showSuggestion(msg: String) {
        val label = panel {
            row {
                icon(AutoDevIcons.Idea).gap(RightGap.SMALL)
                link(msg) {
                    inputSection.text = msg
                    inputSection.requestFocus()

                    suggestionPanel.removeAll()
                    updateUI()
                }.also {
                    it.component.foreground = JBColor.namedColor("Link.activeForeground", JBColor(Gray.x80, Gray.x8C))
                }
            }
        }

        suggestionPanel.add(label)
        updateUI()
    }
}

