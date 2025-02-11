package cc.unitmesh.devti.sketch.ui.code

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.devin.dataprovider.BuiltinCommand
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.editor.ex.MarkupModelEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.ui.JBUI
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiManager
import com.intellij.temporary.gui.block.whenDisposed
import com.intellij.util.ui.JBEmptyBorder
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

open class CodeHighlightSketch(
    open val project: Project,
    open val text: String,
    private var ideaLanguage: Language? = null,
    val editorLineThreshold: Int = 6
) : JBPanel<CodeHighlightSketch>(BorderLayout()), DataProvider, LangSketch, Disposable {
    private val devinLineThreshold = 10
    private val minDevinLineThreshold = 1
    private var isDevIns = false

    private var textLanguage: String? = null

    var editorFragment: EditorFragment? = null
    private var hasSetupAction = false

    init {
        if (text.isNotNullOrEmpty() && (ideaLanguage?.displayName != "Markdown" && ideaLanguage != PlainTextLanguage.INSTANCE)) {
            initEditor(text)
        }
    }

    private fun String?.isNotNullOrEmpty(): Boolean {
        return this != null && this.isNotEmpty()
    }

    fun initEditor(text: String) {
        if (hasSetupAction) return
        hasSetupAction = true

        val editor = createCodeViewerEditor(project, text, ideaLanguage, this)

        border = JBEmptyBorder(8)
        layout = BorderLayout(JBUI.scale(8), 0)

        editor.component.isOpaque = true

        if (ideaLanguage?.displayName == "DevIn") {
            isDevIns = true
            editorFragment = EditorFragment(editor, devinLineThreshold)
        } else {
            editorFragment = EditorFragment(editor, editorLineThreshold)
        }

        add(editorFragment!!.getContent(), BorderLayout.CENTER)

        if (textLanguage != null && textLanguage?.lowercase() != "markdown" && ideaLanguage != PlainTextLanguage.INSTANCE) {
            setupActionBar(project, editor)
        } else {
            editor.backgroundColor = JBColor.PanelBackground
        }
    }

    override fun getViewText(): String {
        return editorFragment?.editor?.document?.text ?: ""
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {
        if (ideaLanguage == null || ideaLanguage == PlainTextLanguage.INSTANCE) {
            ideaLanguage = language
            textLanguage = originLanguage
        }
    }

    override fun updateViewText(text: String, complete: Boolean) {
        if (!hasSetupAction && text.isNotEmpty()) {
            initEditor(text)
        }

        WriteCommandAction.runWriteCommandAction(project) {
            if (editorFragment?.editor?.isDisposed == true) return@runWriteCommandAction

            val document = editorFragment?.editor?.document
            val normalizedText = StringUtil.convertLineSeparators(text)
            document?.replaceString(0, document.textLength, normalizedText)

            document?.lineCount?.let {
                if (it > editorLineThreshold) {
                    editorFragment?.updateExpandCollapseLabel()
                }
                if (complete) {
                    if (isDevIns && it > minDevinLineThreshold) {
                        editorFragment?.resizeForNewThreshold(minDevinLineThreshold)
                    }
                }
            }
        }
    }

    override fun onDoneStream(allText: String) {
        if (ideaLanguage?.displayName == "DevIn") {
            val currentText = getViewText()
            if (currentText.startsWith("/" + BuiltinCommand.WRITE.commandName + ":")) {
                processWriteCommand(currentText)
                return
            }

            val parse = CodeFence.parse(editorFragment!!.editor.document.text)
            var panel: JComponent? = null
            when (parse.originLanguage) {
                "diff", "patch" -> {
                    val langSketch = LanguageSketchProvider.provide("patch")?.create(project, parse.text) ?: return
                    panel = langSketch.getComponent()
                    langSketch.onDoneStream(allText)
                }

                "html" -> {
                    val langSketch = LanguageSketchProvider.provide("html")?.create(project, parse.text) ?: return
                    panel = langSketch.getComponent()
                    langSketch.onDoneStream(allText)
                }

                "bash", "shell" -> {
                    val langSketch = LanguageSketchProvider.provide("shell")?.create(project, parse.text) ?: return
                    panel = langSketch.getComponent()
                    langSketch.onDoneStream(allText)
                }
            }

            if (panel == null) return

            panel.border = JBEmptyBorder(4)
            add(panel, BorderLayout.SOUTH)

            editorFragment?.updateExpandCollapseLabel()

            revalidate()
            repaint()
        }
    }

    override fun getComponent(): JComponent = this

    override fun getData(dataId: String): Any? = null

    companion object {
        private val LINE_NO_REGEX = Regex("^\\d+:")

        fun createCodeViewerEditor(
            project: Project,
            text: String,
            ideaLanguage: Language?,
            disposable: Disposable,
        ): EditorEx {
            var editorText = text
            val language = ideaLanguage ?: CodeFence.findLanguage("Plain text")
            val ext = if (language.displayName == "Plain text") {
                CodeFence.lookupFileExt(language.displayName)
            } else {
                language.associatedFileType?.defaultExtension ?: "Unknown"
            }
            /// check text easyline starts with Lineno and :, for example: 1:
            var isShowLineNo = true
            editorText.lines().forEach {
                if (!it.matches(LINE_NO_REGEX)) {
                    isShowLineNo = false
                    return@forEach
                }
            }

            if (isShowLineNo) {
                val newLines = text.lines().map { it.replace(LINE_NO_REGEX, "") }
                editorText = newLines.joinToString("\n")
            }

            val file = LightVirtualFile("shire.${ext}", language, editorText)
            val document: Document = file.findDocument() ?: throw IllegalStateException("Document not found")

            return createCodeViewerEditor(project, file, document, disposable, isShowLineNo)
        }

        private fun createCodeViewerEditor(
            project: Project,
            file: LightVirtualFile,
            document: Document,
            disposable: Disposable,
            isShowLineNo: Boolean? = false,
        ): EditorEx {
            val editor: EditorEx = ReadAction.compute<EditorEx, Throwable> {
                EditorFactory.getInstance().createViewer(document, project, EditorKind.PREVIEW) as EditorEx
            }

            disposable.whenDisposed {
                EditorFactory.getInstance().releaseEditor(editor)
            }

            editor.setFile(file)
            editor.setCaretEnabled(true)

            val highlighter = ApplicationManager.getApplication()
                .getService(EditorHighlighterFactory::class.java)
                .createEditorHighlighter(project, file)

            editor.highlighter = highlighter

            val markupModel: MarkupModelEx = editor.markupModel
            (markupModel as EditorMarkupModel).isErrorStripeVisible = false

            val settings = editor.settings.also {
                it.isDndEnabled = false
                it.isLineNumbersShown = isShowLineNo == true
                it.additionalLinesCount = 0
                it.isLineMarkerAreaShown = false
                it.isFoldingOutlineShown = false
                it.isRightMarginShown = false
                it.isShowIntentionBulb = false
                it.isUseSoftWraps = true
                it.isRefrainFromScrolling = true
                it.isAdditionalPageAtBottom = false
                it.isCaretRowShown = false
            }

            editor.addFocusListener(object : FocusChangeListener {
                override fun focusGained(focusEditor: Editor) {
                    settings.isCaretRowShown = true
                }

                override fun focusLost(focusEditor: Editor) {
                    if (focusEditor.isDisposed) return
                    if (editor.isDisposed) return

                    settings.isCaretRowShown = false
                    editor.markupModel.removeAllHighlighters()
                }
            })

            return editor
        }
    }

    override fun dispose() {
        editorFragment?.editor?.let {
            EditorFactory.getInstance().releaseEditor(it)
        }

        editorFragment = null
    }
}

private fun CodeHighlightSketch.processWriteCommand(currentText: String) {
    val button = JButton("Write to file").apply {
        preferredSize = JBUI.size(100, 30)

        addActionListener {
            val file = ScratchRootType.getInstance().createScratchFile(
                project,
                "DevIn-${System.currentTimeMillis()}.devin",
                Language.findLanguageByID("DevIn"),
                currentText
            )

            if (file == null) {
                return@addActionListener
            }

            val psiFile = PsiManager.getInstance(project).findFile(file)!!

            RunService.provider(project, file)
                ?.runFile(project, file, psiFile, isFromToolAction = true)
                ?: RunService.runInCli(project, psiFile)
                ?: AutoDevNotifications.notify(project, "No run service found for ${file.name}")
        }
    }

    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
    panel.add(button)

    add(panel, BorderLayout.SOUTH)
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}
