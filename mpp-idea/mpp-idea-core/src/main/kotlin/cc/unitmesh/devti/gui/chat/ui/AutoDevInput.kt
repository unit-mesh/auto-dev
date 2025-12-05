package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.placeholder
import cc.unitmesh.devti.util.InsertUtil
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke


class AutoDevInput(
    project: Project,
    private val listeners: List<DocumentListener>,
    val disposable: Disposable?,
    val inputSection: AutoDevInputSection,
    val showAgent: Boolean = true
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {
    private var editorListeners: EventDispatcher<AutoDevInputListener> = inputSection.editorListeners
    
    // 处理普通 Enter 键提交的 Action
    private val submitAction = DumbAwareAction.create {
        editorListeners.multicaster.onSubmit(inputSection, AutoDevInputTrigger.Key)
    }
    
    private val enterShortcutSet = CustomShortcutSet(KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null))

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
                    // move to next line
                    EditorModificationUtil.moveCaretRelatively(editor, 1)
                } else {
                    document.insertString(caretOffset, eol)
                    editor.caretModel.moveToOffset(caretOffset + eol.length)
                }
            }
        }, "Insert New Line", null)
    }

    init {
        AutoInputService.getInstance(project).registerAutoDevInput(this)
        isOneLineMode = false
        if (showAgent) {
            placeholder("chat.panel.initial.text", this)
        } else {
            placeholder("chat.panel.initial.text.noAgent", this)
        }
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

        // 监听补全弹窗状态，动态注册/注销 Enter 键
        project.messageBus.connect(disposable ?: this).subscribe(LookupManagerListener.TOPIC, object : LookupManagerListener {
            override fun activeLookupChanged(oldLookup: com.intellij.codeInsight.lookup.Lookup?, newLookup: com.intellij.codeInsight.lookup.Lookup?) {
                if (newLookup != null) {
                    // 有补全弹窗时，注销 Enter 键快捷键
                    unregisterEnterShortcut()
                } else {
                    // 没有补全弹窗时，注册 Enter 键快捷键
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
        // when debug or AutoDev show in first, the editorListeners will be null
        editorListeners?.multicaster?.editorAdded((editor as EditorEx))
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

    override fun getData(dataId: String): Any? {
        if (!PlatformCoreDataKeys.FILE_EDITOR.`is`(dataId)) {
            return super.getData(dataId)
        }

        val currentEditor = editor ?: return super.getData(dataId)
        return TextEditorProvider.getInstance().getTextEditor(currentEditor)
    }

    override fun dispose() {
        listeners.forEach {
            editor?.document?.removeDocumentListener(it)
        }

        AutoInputService.getInstance(project).deregisterAutoDevInput(this)
    }

    fun recreateDocument() {
        val language = findLanguage("DevIn")
        val id = UUID.randomUUID()
        val file = LightVirtualFile("AutoDevInput-$id", language, "")

        val document = ReadAction.compute<Document, Throwable> {
            EditorFactory.getInstance().createDocument("")
        }

        initializeDocumentListeners(document)
        setDocument(document)
        inputSection.initEditor()
    }

    private fun initializeDocumentListeners(inputDocument: Document) {
        listeners.forEach { listener ->
            inputDocument.addDocumentListener(listener)
        }
    }

    fun appendText(text: String) {
        WriteCommandAction.runWriteCommandAction(project, "Append text", "intentions.write.action", {
            val document = this.editor?.document ?: return@runWriteCommandAction
            InsertUtil.insertStringAndSaveChange(project, text, document, document.textLength, false)
        })
    }
}

fun VirtualFile.relativePath(project: Project): String {
    if (this is LightVirtualFile) return ""
    try {
        val projectDir = project.guessProjectDir()!!.toNioPath().toFile()
        val relativePath = FileUtil.getRelativePath(projectDir, this.toNioPath().toFile())
        return relativePath ?: this.path
    } catch (e: Exception) {
        return this.path
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}
