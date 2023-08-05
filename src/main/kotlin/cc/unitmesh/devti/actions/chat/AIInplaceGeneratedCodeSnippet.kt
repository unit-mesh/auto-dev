package cc.unitmesh.devti.actions.chat

import cc.unitmesh.devti.gui.block.CodeBlockView
import cc.unitmesh.devti.gui.block.CodePartEditorInfo
import cc.unitmesh.devti.parser.Code
import com.intellij.ide.KeyboardAwareFocusOwner
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

class AIInplaceGeneratedCodeSnippet(state: State = State.WAITING_INITIAL_RESPONSE) : JPanel(),
    KeyboardAwareFocusOwner {
    private var state: State
    private var editorInfo: CodePartEditorInfo? = null
    private val generatingPlaceholder: Component
    private val actionsBar: JComponent
    private val acceptButton: JButton
    private val cancelButton: JButton
    private val specifyButton: JButton
    private val regenerateButton: JButton

    enum class State {
        WAITING_INITIAL_RESPONSE,
        GENERATING,
        GENERATION_COMPLETED
    }

    init {
        this.state = state
        generatingPlaceholder = JLabel("Code generation in progress...")
        actionsBar = JPanel()
        acceptButton = JButton("Accept")
        cancelButton = JButton("Cancel")
        specifyButton = JButton("Specify")
        regenerateButton = JButton("Regenerate")
        initComponent()
        updateActions()
    }

    val code: String?
        get() {
            val codePartEditorInfo = editorInfo ?: return null
            return codePartEditorInfo.code.get()
        }

    override fun skipKeyEventDispatcher(event: KeyEvent): Boolean {
        return true
    }

    fun setCode(code: Code, project: Project, disposable: Disposable) {
        updateOrCreateCodeView(code, project, disposable)
        val isComplete: Boolean = code.isComplete
        this.state = if (!isComplete) {
            State.GENERATING
        } else {
            State.GENERATION_COMPLETED
        }

        updateActions()
    }

    private fun updateOrCreateCodeView(code: Code, project: Project, disposable: Disposable): CodePartEditorInfo? {
        var editorInfo = editorInfo
        if (editorInfo == null) {
            editorInfo = CodeBlockView.createStrippedCodeViewer(
                project,
                PropertyGraph(null as String?, false).property(code.text),
                disposable,
                code.language
            )

            customizeEditor(editorInfo.editor)
            val components = components
            val replaceIndex = components.indexOf(generatingPlaceholder)
            remove(replaceIndex)
            add(editorInfo.component as Component, replaceIndex)
            this.editorInfo = editorInfo
        } else {
            if (editorInfo.language == code.language) {
                editorInfo.language = code.language
            }
            editorInfo.code.set(code.text)
        }

        return editorInfo
    }

    fun addAcceptCallback(callback: () -> Unit) {
        acceptButton.addActionListener { callback.invoke() }
    }

    fun addSpecifyCallback(callback: () -> Unit) {
        specifyButton.addActionListener { callback.invoke() }
    }

    fun addRegenerateCallback(callback: () -> Unit) {
        regenerateButton.addActionListener { callback.invoke() }
    }

    fun addCancelCallback(callback: () -> Unit) {
        cancelButton.addActionListener { callback.invoke() }
    }

    private fun initComponent() {
        setBackground(ColorUtil.withAlpha(JBColor.GREEN, 0.1))
        add(generatingPlaceholder)
        add(actionsBar as Component)
    }

    private fun customizeEditor(editor: EditorEx) {
        editor.component.setOpaque(false)
        editor.backgroundColor = ColorUtil.withAlpha(editor.backgroundColor, 0.0)
        editor.scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
        editor.scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    }

    private fun updateActions() {
        actionsBar.removeAll()
        when (this.state) {
            State.WAITING_INITIAL_RESPONSE -> this.actionsBar.add(this.cancelButton)
            State.GENERATING -> this.actionsBar.add(this.cancelButton)
            State.GENERATION_COMPLETED -> {
                val actionBar = this.actionsBar
                actionBar.add(this.acceptButton)
                actionBar.add(this.specifyButton)
                actionBar.add(this.regenerateButton)
                actionBar.add(this.cancelButton)
            }
        }
    }
}
