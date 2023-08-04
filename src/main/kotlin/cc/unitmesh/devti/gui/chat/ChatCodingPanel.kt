package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.gui.block.whenDisposed
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.NullableComponent
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.ScrollPaneConstants


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
    init {
        focusMouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                focusInput()
            }
        }

        myTitle.foreground = JBColor.namedColor("Label.infoForeground", JBColor(Gray.x80, Gray.x8C))
        myTitle.font = JBFont.label()

        myList.isOpaque = true
        myList.background = UIUtil.getListBackground()

        myScrollPane = JBScrollPane(
            myList,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        myScrollPane.border = JBEmptyBorder(10, 15, 10, 15)
        myScrollPane.verticalScrollBar.autoscrolls = true

        progressBar = JProgressBar()

        val actionLink = ActionLink(AutoDevBundle.message("label.submit.issue")) {
            BrowserUtil.browse("https://github.com/unit-mesh/auto-dev/issues")
        }
        actionLink.setExternalLinkIcon()

        inputSection = AutoDevInputSection(chatCodingService.project, disposable)
        inputSection.addListener(object : AutoDevInputListener {
            override fun onSubmit(component: AutoDevInputSection, trigger: AutoDevInputTrigger) {
                val prompt = component.text
                component.text = ""
                val context = ChatContext(null, "", "")

                chatCodingService.actionType = ChatActionType.CHAT
                chatCodingService.handlePromptAndResponse(this@ChatCodingPanel, object : ContextPrompter() {
                    override fun displayPrompt() = prompt
                    override fun requestPrompt() = prompt
                }, context)
            }
        })

        panelContent = panel {
                row {
                    cell(myScrollPane)
                        .verticalAlign(VerticalAlign.FILL)
                        .horizontalAlign(HorizontalAlign.FILL)
                }.resizableRow()

                row {
                    cell(progressBar).horizontalAlign(HorizontalAlign.FILL)
                }

                row {
                    cell(actionLink).horizontalAlign(HorizontalAlign.RIGHT)
                }

                row {
                    cell(inputSection).horizontalAlign(HorizontalAlign.FILL)
                }
            }

        setContent(panelContent)

        inputSection.text = ""

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

        progressBar.isVisible = true

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

        messageView.reRenderAssistantOutput()

        return text
    }

    fun setInput(trimMargin: String) {
        inputSection.text = trimMargin
        this.focusInput()
    }
}