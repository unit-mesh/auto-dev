package cc.unitmesh.devti.inline

import cc.unitmesh.devti.llms.LlmFactory
import cc.unitmesh.devti.llms.cancelHandler
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import com.intellij.icons.AllIcons
import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.util.ui.JBUI.CurrentTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import java.awt.*
import java.awt.event.*
import java.awt.geom.Rectangle2D
import javax.swing.*

class AutoDevInlineChatPanel(val editor: Editor) : JPanel(GridBagLayout()), EditorCustomElementRenderer,
    Disposable {
    var inlay: Inlay<*>? = null
    val inputPanel = AutoDevInlineChatInput(this, onSubmit = { input ->
        this.centerPanel.isVisible = true
        val project = editor.project!!

        val prompt = AutoDevInlineChatService.getInstance().prompting(project, input, editor)
        val flow: Flow<String>? = LlmFactory.instance.create(project).stream(prompt, "", false)

        val panelView = SketchToolWindow(project, editor)
        panelView.minimumSize = Dimension(800, 40)
        addToCenter(panelView)

        AutoDevCoroutineScope.scope(project).launch {
            val suggestion = StringBuilder()
            panelView.onStart()

            flow?.cancelHandler { panelView.handleCancel = it }?.cancellable()?.collect { char ->
                suggestion.append(char)

                invokeLater {
                    panelView.onUpdate(suggestion.toString())
                    panelView.resize()
                }
            }

            panelView.resize()
            panelView.onFinish(suggestion.toString())
        }
    })
    private var centerPanel: JPanel = JPanel(BorderLayout())
    private var container: Container? = null

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0),
                RoundedLineBorder(JBColor.LIGHT_GRAY, 12, 1)
            ),
            BorderFactory.createCompoundBorder(
                AutoDevLineBorder(JBColor.border(), 1, true, 8),
                BorderFactory.createMatteBorder(6, 8, 6, 8, JBColor.border())
            )
        )

        isOpaque = false

        val c = GridBagConstraints()
        c.gridx = 0
        c.gridy = 0
        c.weightx = 1.0
        c.fill = GridBagConstraints.HORIZONTAL
        add(this.inputPanel, c)

        this.centerPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            this.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    IdeFocusManager.getInstance(editor.project).requestFocus(inputPanel.getInputComponent(), true)
                }
            })
        }

        c.gridx = 0
        c.gridy = 1
        c.fill = GridBagConstraints.BOTH
        add(this.centerPanel, c)

        this.inAllChildren { child ->
            child.addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    this@AutoDevInlineChatPanel.redraw()
                }
            })
        }
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int = size.width

    override fun calcHeightInPixels(inlay: Inlay<*>): Int = size.height

    private fun redraw() {
        ApplicationManager.getApplication().invokeLater {
            if (this.size.height != this.getMinimumSize().height) {
                this.size = Dimension(800, this.getMinimumSize().height)
                this.inlay?.update()

                this.revalidate()
                this.repaint()
            }
        }
    }

    fun createInlay(offset: Int) {
        inlay = editor.inlayModel.addBlockElement(offset, false, true, 1, this)
    }

    fun setInlineContainer(container: Container) {
        this.container = container
    }

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        bounds = inlay.bounds ?: return
        revalidate()
        repaint()
    }

    private fun addToCenter(content: JComponent) {
        content.isOpaque = true
        ApplicationManager.getApplication().invokeLater {
            if (!this.centerPanel.isVisible) {
                this.centerPanel.isVisible = true
            }

            this.centerPanel.removeAll()
            this.centerPanel.add(content, BorderLayout.CENTER)

            this@AutoDevInlineChatPanel.redraw()
        }
    }

    fun JComponent.inAllChildren(callback: (JComponent) -> Unit) {
        callback(this)
        components.forEach { component ->
            if (component is JComponent) {
                component.inAllChildren(callback)
            }
        }
    }

    override fun dispose() {
        inputPanel.dispose()
        inlay?.dispose()
        inlay = null
    }
}

class AutoDevInlineChatInput(
    val autoDevInlineChatPanel: AutoDevInlineChatPanel,
    val onSubmit: (String) -> Unit,
) : JPanel(GridBagLayout()), Disposable {
    private val textArea: JBTextArea

    init {
        layout = BorderLayout()
        textArea = object : JBTextArea(), KeyboardAwareFocusOwner {
            override fun skipKeyEventDispatcher(event: KeyEvent): Boolean = true

            init {
                isOpaque = true
                isFocusable = true
                lineWrap = true
                wrapStyleWord = true
                border = BorderFactory.createEmptyBorder(8, 6, 8, 6)
            }
        }

        border = AutoDevLineBorder(CurrentTheme.Focus.focusColor(), 1, true, 2)

        // escape to close
        textArea.actionMap.put("escapeAction", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                AutoDevInlineChatService.getInstance().closeInlineChat(autoDevInlineChatPanel.editor)
            }
        })
        textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeAction")
        // submit with enter
        textArea.actionMap.put("enterAction", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                submit()
            }
        })
        textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterAction")
        // newLine with shift + enter
        textArea.actionMap.put("newlineAction", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                textArea.append("\n")
            }
        })
        textArea.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "newlineAction")

        val submitPresentation = Presentation("Submit")
        submitPresentation.icon = AllIcons.Actions.Execute
        val submitButton = ActionButton(
            DumbAwareAction.create { submit() },
            submitPresentation, "", Dimension(40, 20)
        )

        add(textArea)
        add(submitButton, BorderLayout.EAST)
    }

    private fun submit() {
        val trimText = textArea.text.trim()
        textArea.text = ""
        onSubmit(trimText)
    }

    fun getInputComponent(): Component = textArea

    override fun dispose() {

    }
}

fun <T : JComponent> Cell<T>.fullWidth(): Cell<T> {
    return this.align(AlignX.FILL)
}

fun <T : JComponent> Cell<T>.fullHeight(): Cell<T> {
    return this.align(AlignY.FILL)
}
