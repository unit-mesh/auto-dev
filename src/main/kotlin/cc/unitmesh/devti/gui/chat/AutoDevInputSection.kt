package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.llms.tokenizer.LLM_MAX_TOKEN
import cc.unitmesh.devti.llms.tokenizer.Tokenizer
import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
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
import java.awt.EventQueue.invokeLater
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.function.Supplier
import javax.swing.Box
import javax.swing.JComponent
import kotlin.math.max
import kotlin.math.min

class AutoDevInputSection(private val project: Project, val disposable: Disposable?) : BorderLayoutPanel() {
    private val input: AutoDevInput
    private val documentListener: DocumentListener
    private val buttonPresentation: Presentation
    private val button: ActionButton
    private val dispatcher: EventDispatcher<AutoDevInputListener> =
        EventDispatcher.create(AutoDevInputListener::class.java)
    private var tokenizer: Tokenizer? = null
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


        input = AutoDevInput(project, listOf(), disposable, this)
        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val i = input.preferredSize?.height
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

        ComponentValidator(disposable!!).withValidator(Supplier<ValidationInfo?> {
            val validationInfo: ValidationInfo? = this.getInputValidationInfo()
            button.setEnabled(validationInfo == null)
            return@Supplier validationInfo
        }).installOn((this as JComponent)).revalidate()

        addListener(object : AutoDevInputListener {
            override fun editorAdded(editor: EditorEx) {
                this@AutoDevInputSection.initEditor()
            }
        })

        tokenizer = TokenizerImpl.INSTANCE
    }

    fun initEditor() {
        val editorEx = when (this.input.editor) {
            is EditorEx -> this.input.editor
            else -> null
        } ?: return

        val jComponent = this as JComponent

        setBorder(AutoDevCoolBorder(editorEx as EditorEx, jComponent))
        UIUtil.forEachComponentInHierarchy(jComponent) { component: Component ->
            (component as JComponent).setOpaque(false)
        }
    }


    override fun getPreferredSize(): Dimension {
        val result = super.getPreferredSize()
        result.height = max(min(result.height, maxHeight), minimumSize.height)
        return result
    }

    fun setContent(trimMargin: String) {
        val focusManager = IdeFocusManager.getInstance(project)
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

    private fun getInputValidationInfo(): ValidationInfo? {
        val text = input.getDocument().text
        val textLength = (this.tokenizer)?.count(text) ?: text.length

        val exceed: Int = textLength - LLM_MAX_TOKEN
        if (exceed <= 0) return null

        val errorMessage = AutoDevBundle.message("chat.too.long.user.message", exceed)
        return ValidationInfo(errorMessage, this as JComponent).asWarning()
    }

    private val maxHeight: Int
        get() {
            val decorator: InternalDecorator = UIUtil.getParentOfType(
                InternalDecorator::class.java, this as Component
            )!!

            val contentManager: ContentManager = decorator.contentManager ?: return JBUI.scale(200)
            return contentManager.component.height / 2
        }

    val focusableComponent: JComponent get() = input
}