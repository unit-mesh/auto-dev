package cc.unitmesh.devti.gui.chat.ui

import cc.unitmesh.devti.settings.LanguageChangedCallback.placeholder
import cc.unitmesh.devti.util.parser.CodeFence.Companion.findLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.actions.IncrementalFindAction
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.temporary.gui.block.findDocument
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.util.EventDispatcher
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke


class AutoDevInput(
    project: Project,
    private val listeners: List<DocumentListener>,
    val disposable: Disposable?,
    val inputSection: AutoDevInputSection,
) : EditorTextField(project, FileTypes.PLAIN_TEXT), Disposable {
    private var editorListeners: EventDispatcher<AutoDevInputListener> = inputSection.editorListeners

    init {
        AutoInputService.getInstance(project).registerAutoDevInput(this)
        isOneLineMode = false
        placeholder("chat.panel.initial.text", this)
        setFontInheritedFromLAF(true)
        addSettingsProvider {
            it.putUserData(IncrementalFindAction.SEARCH_DISABLED, true)
            it.colorsScheme.lineSpacing = 1.2f
            it.settings.isUseSoftWraps = true
            it.isEmbeddedIntoDialogWrapper = true
            it.contentComponent.setOpaque(false)
        }

        DumbAwareAction.create {
            val editor = editor ?: return@create
            // Insert a new line
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
        }.registerCustomShortcutSet(
            CustomShortcutSet(
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.META_DOWN_MASK), null),
                KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), null),
            ), this
        )

        val connect: MessageBusConnection = project.messageBus.connect(disposable ?: this)
        val topic = AnActionListener.TOPIC
        connect.subscribe(topic, object : AnActionListener {
            override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
                if (event.dataContext.getData(CommonDataKeys.EDITOR) === editor && action is EnterAction) {
                    editorListeners.multicaster.onSubmit(inputSection, AutoDevInputTrigger.Key)
                }
            }
        })

        listeners.forEach { listener ->
            document.addDocumentListener(listener)
        }
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

        val document = file.findDocument() ?: throw IllegalStateException("Can't create in-memory document")

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
        WriteCommandAction.runWriteCommandAction(
            project,
            "Append text",
            "intentions.write.action",
            {
                val document = this.editor?.document ?: return@runWriteCommandAction
                insertStringAndSaveChange(project, text, document, document.textLength, false)
            })
    }
}

fun insertStringAndSaveChange(
    project: Project,
    content: String,
    document: Document,
    startOffset: Int,
    withReformat: Boolean,
) {
    if (startOffset < 0 || startOffset > document.textLength) return

    document.insertString(startOffset, content)
    PsiDocumentManager.getInstance(project).commitDocument(document)

    if (!withReformat) return

    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
    psiFile?.let { file ->
        val reformatRange = TextRange(startOffset, startOffset + content.length)
        CodeStyleManager.getInstance(project).reformatText(file, listOf(reformatRange))
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
