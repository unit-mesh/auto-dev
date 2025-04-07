package cc.unitmesh.devti.gui.chat

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.agent.custom.model.CustomAgentConfig
import cc.unitmesh.devti.alignRight
import cc.unitmesh.devti.fullHeight
import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.chat.message.ChatContext
import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputListener
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputSection
import cc.unitmesh.devti.gui.chat.ui.AutoDevInputTrigger
import cc.unitmesh.devti.gui.chat.view.MessageView
import cc.unitmesh.devti.gui.toolbar.CopyAllMessagesAction
import cc.unitmesh.devti.gui.toolbar.NewChatAction
import cc.unitmesh.devti.provider.TextContextPrompter
import cc.unitmesh.devti.provider.devins.LanguageProcessor
import cc.unitmesh.devti.settings.AutoDevSettingsState
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.createActionButton
import cc.unitmesh.devti.sketch.ui.code.HtmlHighlightSketch
import cc.unitmesh.devti.util.whenDisposed
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBColor.PanelBackground
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

interface AutoDevChatPanel {
    val progressBar: JProgressBar get() = JProgressBar()
    fun resetChatSession()

    /**
     * Custom Agent Event
     */
    fun resetAgent()
    fun hasSelectedCustomAgent(): Boolean
    fun getSelectedCustomAgent(): CustomAgentConfig
    fun selectAgent(config: CustomAgentConfig)

    /**
     * Progress Bar
     */
    fun hiddenProgressBar()
    fun showProgressBar()

    /**
     * append custom view
     */
    fun appendWebView(content: String, project: Project)
}

class NormalChatCodingPanel(private val chatCodingService: ChatCodingService, val disposable: Disposable?) :
    SimpleToolWindowPanel(true, true), NullableComponent, AutoDevChatPanel {
    private val myList = JPanel(VerticalLayout(JBUI.scale(4)))
    private var inputSection: AutoDevInputSection
    private val focusMouseListener: MouseAdapter
    private var panelContent: DialogPanel
    private val myScrollPane: JBScrollPane
    private val delaySeconds: String get() = AutoDevSettingsState.getInstance().delaySeconds

    init {
        focusMouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                focusInput()
            }
        }

        myScrollPane = JBScrollPane(
            myList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        myScrollPane.verticalScrollBar.autoscrolls = true

        inputSection = AutoDevInputSection(chatCodingService.project, disposable).apply {
            border = JBUI.Borders.empty(8)
        }
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

                val task = object : Task.Backgroundable(chatCodingService.project, "Compile context", false) {
                    override fun run(indicator: ProgressIndicator) {
                        runBlocking {
                            val postProcessors = LanguageProcessor.devin()
                            if (postProcessors != null) {
                                prompt = postProcessors.compile(chatCodingService.project, prompt)
                            }

                            chatCodingService.actionType = ChatActionType.CHAT
                            chatCodingService.handlePromptAndResponse(
                                this@NormalChatCodingPanel,
                                TextContextPrompter(prompt),
                                ChatContext(),
                                false
                            )
                        }
                    }
                }
                ProgressManager.getInstance()
                    .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
            }
        })
        val header = panel {
            row {
                val buttonBox = Box.createHorizontalBox()
                buttonBox.add(Box.createHorizontalGlue())
                buttonBox.add(createActionButton(NewChatAction()))
                buttonBox.add(createActionButton(CopyAllMessagesAction()))
                cell(buttonBox).alignRight()
            }
        }

        header.border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0)

        panelContent = panel {
            row { cell(header).fullWidth() }
            row { cell(myScrollPane).fullWidth().fullHeight() }.resizableRow()
            row { cell(progressBar).fullWidth() }
            row { cell(inputSection).fullWidth() }
        }.also {
            it.border = JBUI.Borders.empty()
            it.background = PanelBackground
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

        val messageView = MessageView(chatCodingService.project, message, role, displayText)
        runInEdt {
            myList.add(messageView)
        }

        scrollToBottom()
        progressBar.isIndeterminate = true
        updateUI()
        return messageView
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
        showProgressBar()
        val text = updateMessageInUi(content)

        progressBar.isIndeterminate = false
        hiddenProgressBar()
        updateUI()

        postAction(text)
    }

    private suspend fun updateMessageInUi(content: Flow<String>): String {
        val messageView = MessageView(chatCodingService.project, "", ChatRole.Assistant, "")
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

        if (buffer.isNotEmpty()) {
            text += buffer.joinToString("")
            messageView.updateContent(text)
        }

        if (delaySeconds.isNotBlank()) {
            val elapsedTime = System.currentTimeMillis() - startTime
            withContext(Dispatchers.IO) {
                delaySeconds.toLongOrNull()?.let {
                    val remainingTime = maxOf(it * 1000 - elapsedTime, 0)
                    delay(remainingTime)
                }
            }
        }

        messageView.onFinish(text)
        return text
    }

    fun setInput(trimMargin: String) {
        inputSection.text = trimMargin.trim()
        this.focusInput()
    }

    /**
     * Resets the chat session by clearing the current session and updating the UI.
     */
    override fun resetChatSession() {
        chatCodingService.stop()
        chatCodingService.clearSession()
        myList.removeAll()
        this.hiddenProgressBar()
        this.resetAgent()
        updateUI()
    }

    override fun resetAgent() {
        inputSection.resetAgent()
    }

    override fun hasSelectedCustomAgent(): Boolean {
        return inputSection.hasSelectedAgent()
    }

    override fun getSelectedCustomAgent(): CustomAgentConfig {
        return inputSection.getSelectedAgent()
    }

    override fun selectAgent(config: CustomAgentConfig) {
        inputSection.selectAgent(config)
    }

    override fun hiddenProgressBar() {
        progressBar.isVisible = false
        inputSection.showSendButton()
    }

    override fun showProgressBar() {
        progressBar.isVisible = true
        inputSection.showStopButton()
    }

    fun removeLastMessage() {
        if (myList.componentCount > 0) {
            myList.remove(myList.componentCount - 1)
        }

        updateUI()
    }

    override fun appendWebView(content: String, project: Project) {
        myList.add(HtmlHighlightSketch(project, content).getComponent())
        updateUI()
    }

    fun moveCursorToStart() {
        inputSection.moveCursorToStart()
    }
}

