package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.provider.ContextPrompter
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.impl.InternalDecorator
import com.intellij.ui.content.ContentManager
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JComponent
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
    private val buttonPresentation: Presentation
    private val button: ActionButton
    private val dispatcher: EventDispatcher<AutoDevInputListener> =
        EventDispatcher.create(AutoDevInputListener::class.java)
    var text: String
        get() {
            return input.text
        }
        set(text) {
            input.recreateDocument()
            input.text = text
        }


    init {
        val presentation = Presentation(AutoDevBundle.message("devti.chat.send"))
        presentation.setIcon(AutoDevIcons.Send)
        buttonPresentation = presentation
        button = ActionButton(
            DumbAwareAction.create {
                object : DumbAwareAction("") {
                    override fun actionPerformed(e: AnActionEvent) {
                        dispatcher.multicaster.onSubmit(this@AutoDevInputSection, AutoDevInputTrigger.Button)
                    }
                }.actionPerformed(it)
            },
            buttonPresentation,
            "",
            Dimension(20, 20)
        )


        input = AutoDevInputField(project, listOf(), disposable, this)
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
        borderLayoutPanel.addToRight(button)
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

    override fun getBackground(): Color? {
        // it seems that the input field is not ready when this method is called
        if (this.input == null) return super.getBackground()

        val editor = input.editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun setBackground(bg: Color?) {}

    fun addListener(listener: AutoDevInputListener) {
        dispatcher.addListener(listener)
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