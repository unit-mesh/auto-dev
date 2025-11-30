package cc.unitmesh.devins.idea.editor

import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.util.EventDispatcher
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

/**
 * DevIn language input component for mpp-idea module.
 * 
 * Features:
 * - DevIn language support with syntax highlighting and completion
 * - Enter to submit, Shift/Ctrl/Cmd+Enter for newline
 * - Integration with IntelliJ's completion system (lookup listener)
 * - Placeholder text support
 * 
 * Based on AutoDevInput from core module but adapted for standalone mpp-idea usage.
 */
class IdeaDevInInput(
    project: Project,
    private val listeners: List<DocumentListener> = emptyList(),
    val disposable: Disposable?,
    private val showAgent: Boolean = true
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {

    private val editorListeners = EventDispatcher.create(IdeaInputListener::class.java)

    // Internal document listener to notify text changes
    private val internalDocumentListener = object : DocumentListener {
        override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
            editorListeners.multicaster.onTextChanged(text)
        }
    }

    // Enter key handling - submit on Enter, newline on Shift/Ctrl/Cmd+Enter
    private val submitAction = DumbAwareAction.create {
        val text = text.trim()
        if (text.isNotEmpty()) {
            editorListeners.multicaster.onSubmit(text, IdeaInputTrigger.Key)
        }
    }

    private val enterShortcutSet = CustomShortcutSet(
        KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null)
    )

    private val newlineAction = DumbAwareAction.create {
        val editor = editor ?: return@create
        insertNewLine(editor)
    }

    private fun insertNewLine(editor: Editor) {
        CommandProcessor.getInstance().executeCommand(project, {
            val eol = "\n"
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            val lineEndOffset = document.getLineEndOffset(document.getLineNumber(caretOffset))
            val textAfterCaret = document.getText(TextRange(caretOffset, lineEndOffset))

            WriteCommandAction.runWriteCommandAction(project) {
                if (textAfterCaret.isBlank()) {
                    document.insertString(caretOffset, eol)
                    EditorModificationUtil.moveCaretRelatively(editor, 1)
                } else {
                    document.insertString(caretOffset, eol)
                    editor.caretModel.moveToOffset(caretOffset + eol.length)
                }
            }
        }, "Insert New Line", null)
    }

    init {
        isOneLineMode = false
        setPlaceholder("Type your message or /help for commands...")
        setFontInheritedFromLAF(true)
        
        addSettingsProvider {
            it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
            it.colorsScheme.lineSpacing = 1.2f
            it.settings.isUseSoftWraps = true
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        background = EditorColorsManager.getInstance().globalScheme.defaultBackground

        registerEnterShortcut()
        
        // Register newline shortcuts: Ctrl+Enter, Cmd+Enter, Shift+Enter
        newlineAction.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), null),
            ), this
        )

        listeners.forEach { listener ->
            document.addDocumentListener(listener)
        }

        // Add internal document listener to notify text changes
        document.addDocumentListener(internalDocumentListener)

        // Listen for completion popup state to disable Enter submit when completing
        project.messageBus.connect(disposable ?: this)
            .subscribe(LookupManagerListener.TOPIC, object : LookupManagerListener {
                override fun activeLookupChanged(
                    oldLookup: com.intellij.codeInsight.lookup.Lookup?,
                    newLookup: com.intellij.codeInsight.lookup.Lookup?
                ) {
                    if (newLookup != null) {
                        unregisterEnterShortcut()
                    } else {
                        registerEnterShortcut()
                    }
                }
            })
    }

    private fun registerEnterShortcut() {
        submitAction.registerCustomShortcutSet(enterShortcutSet, this)
    }

    private fun unregisterEnterShortcut() {
        submitAction.unregisterCustomShortcutSet(this)
    }

    override fun onEditorAdded(editor: Editor) {
        editorListeners.multicaster.editorAdded(editor as EditorEx)
    }

    public override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.setVerticalScrollbarVisible(true)
        setBorder(JBUI.Borders.empty())
        editor.setShowPlaceholderWhenFocused(true)
        editor.caretModel.moveToOffset(0)
        editor.scrollPane.setBorder(border)
        editor.contentComponent.setOpaque(false)
        return editor
    }

    override fun getBackground(): Color {
        val editor = editor ?: return super.getBackground()
        return editor.colorsScheme.defaultBackground
    }

    override fun dispose() {
        editor?.document?.removeDocumentListener(internalDocumentListener)
        listeners.forEach {
            editor?.document?.removeDocumentListener(it)
        }
    }

    /**
     * Recreate the document with DevIn language support.
     * This enables syntax highlighting and completion for DevIn commands.
     */
    fun recreateDocument() {
        // Remove listeners from old document before replacing
        editor?.document?.let { oldDoc ->
            oldDoc.removeDocumentListener(internalDocumentListener)
            listeners.forEach { listener ->
                oldDoc.removeDocumentListener(listener)
            }
        }

        // Create new document using EditorFactory
        val document = ReadAction.compute<Document, Throwable> {
            EditorFactory.getInstance().createDocument("")
        }

        initializeDocumentListeners(document)
        setDocument(document)
    }

    private fun initializeDocumentListeners(inputDocument: Document) {
        listeners.forEach { listener ->
            inputDocument.addDocumentListener(listener)
        }
        // Re-add internal listener to new document
        inputDocument.addDocumentListener(internalDocumentListener)
    }

    /**
     * Add a listener for input events.
     */
    fun addInputListener(listener: IdeaInputListener) {
        editorListeners.addListener(listener)
    }

    /**
     * Remove a listener.
     */
    fun removeInputListener(listener: IdeaInputListener) {
        editorListeners.removeListener(listener)
    }

    /**
     * Append text at the end of the document.
     */
    fun appendText(textToAppend: String) {
        WriteCommandAction.runWriteCommandAction(project, "Append text", "intentions.write.action", {
            val document = this.editor?.document ?: return@runWriteCommandAction
            document.insertString(document.textLength, textToAppend)
        })
    }

    /**
     * Clear the input and recreate document.
     */
    fun clearInput() {
        recreateDocument()
    }
}

