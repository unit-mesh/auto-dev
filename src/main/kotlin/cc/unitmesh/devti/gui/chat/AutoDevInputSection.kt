package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.min

class AutoDevInputSection(
    private val chatCodingService: ChatCodingService,
    private val project: Project,
    val disposable: Disposable?,
    val panel: ChatCodingComponent
) : BorderLayoutPanel() {
    private val input: AutoDevInputField
    private val documentListener: DocumentListener

    init {
        val presentation = Presentation(AutoDevBundle.message("devti.chat.send"))
        val jButton = JButton(AutoDevBundle.message("devti.chat.send"))
        presentation.icon = jButton.icon

        input = AutoDevInputField(project, listOf(), disposable)
        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = input.preferredSize.height
                if (i != input.height) {
                    revalidate()
                }
            }
        }

        input.addDocumentListener(documentListener)
        input.recreateDocument()

        val listener: (ActionEvent) -> Unit = {
            val prompt = input.text
            input.text = ""
            val context = ChatContext(null, "", "")

            chatCodingService.actionType = ChatActionType.CHAT
            chatCodingService.handlePromptAndResponse(panel, object : ContextPrompter() {
                override fun displayPrompt() = prompt
                override fun requestPrompt() = prompt
            }, context)
        }

        input.addListener(object : AutoDevInputListener {
            override fun onSubmit(component: AutoDevInputField, trigger: AutoDevInputTrigger) {
                listener.invoke(ActionEvent(component, 0, trigger.name))
            }
        })

        addToCenter(input)

        val borderLayoutPanel = BorderLayoutPanel()
        val horizontalGlue = Box.createHorizontalGlue()
        horizontalGlue.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val ideFocusManager = IdeFocusManager.getInstance(project)
                ideFocusManager.requestFocus(input, true)
            }
        })

        borderLayoutPanel.addToCenter(horizontalGlue)
        borderLayoutPanel.addToRight(jButton)
        addToBottom(borderLayoutPanel)
    }

    override fun getPreferredSize(): Dimension {
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun setContent(trimMargin: String) {
        val focusManager = IdeFocusManager.getInstance(chatCodingService.project)
        focusManager.requestFocus(input, true)
        this.input.recreateDocument()
        this.input.text = trimMargin
    }

    fun setSendingMode(sendingMode: Boolean) {
//        input.setSendingMode(sendingMode)
    }

    private val maxHeight: Int
        get() {
            val decorator: InternalDecorator = UIUtil.getParentOfType(
                InternalDecorator::class.java, this as Component
            )!!

            val contentManager: ContentManager? = decorator.getContentManager()
            if (contentManager != null) {
                val component = contentManager.component
                val toolWindowHeight = component.height
                return toolWindowHeight / 2
            }

            return JBUI.scale(200)
        }
    val focusableComponent: JComponent
        get() = input
}