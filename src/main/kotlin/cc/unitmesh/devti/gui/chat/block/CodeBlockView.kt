package cc.unitmesh.devti.gui.chat.block

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.parser.Code
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.application.runReadAction
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
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import kotlin.jvm.internal.Ref

class CodeBlockView(private val block: CodeBlock, private val project: Project, private val disposable: Disposable) :
    MessageBlockView {
    private var editorInfo: CodePartEditorInfo? = null

    init {
        getBlock().addTextListener {
            if (editorInfo == null) return@addTextListener
            updateOrCreateCodeView()
        }
    }

    override fun getBlock(): CodeBlock {
        return block
    }

    override fun getComponent(): JComponent {
        return editorInfo?.component ?: return updateOrCreateCodeView()!!.component
    }

    val codeContent: String
        get() {
            return editorInfo?.code?.get() ?: ""
        }

    override fun initialize() {
        if (editorInfo == null) {
            updateOrCreateCodeView()
        }
    }

    fun updateOrCreateCodeView(): CodePartEditorInfo? {
        val code: Code = getBlock().code
        if (editorInfo == null) {
            val editorInfo: CodePartEditorInfo = createCodeViewer(
                project,
                PropertyGraph(null as String?, false).property(code.text),
                disposable,
                code.language,
                getBlock().getMessage()
            )
            this.editorInfo = editorInfo
        } else {
            val codePartEditorInfo = editorInfo
            if (codePartEditorInfo!!.language == code.language) {
                editorInfo!!.language = code.language
            }

            editorInfo!!.code.set(code.text)
        }

        return editorInfo
    }

    companion object {
        private fun createCodeViewerFile(language: Language, content: String): LightVirtualFile {
            val file = LightVirtualFile("AutoDevSnippet", language, content)
            if (file.fileType == UnknownFileType.INSTANCE) {
                file.fileType = PlainTextFileType.INSTANCE
            }

            return file
        }

        private fun createCodeViewerEditor(
            project: Project,
            file: LightVirtualFile,
            document: Document,
            disposable: Disposable
        ): EditorEx {
            val editor: Editor = EditorFactory.getInstance().createViewer(document, project, EditorKind.PREVIEW)
            (editor as EditorEx).setFile(file)
            editor.setCaretEnabled(true)
            val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, file)

            editor.highlighter = highlighter

            val markupModel: MarkupModelEx = editor.markupModel
            (markupModel as EditorMarkupModel).isErrorStripeVisible = false

            val editorSettings = editor.getSettings()
            editorSettings.isDndEnabled = false
            editorSettings.isLineNumbersShown = false
            editorSettings.additionalLinesCount = 0
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isFoldingOutlineShown = false
            editorSettings.isRightMarginShown = false
            editorSettings.isShowIntentionBulb = false
            editorSettings.isUseSoftWraps = true
            editorSettings.setPaintSoftWraps(false)
            editorSettings.isRefrainFromScrolling = true
            editorSettings.isAdditionalPageAtBottom = false
            editorSettings.isCaretRowShown = false

            editor.addFocusListener(object : FocusChangeListener {
                override fun focusGained(focusEditor: Editor) {
                    editor.getSettings().isCaretRowShown = true
                }

                override fun focusLost(focusEditor: Editor) {
                    editor.getSettings().isCaretRowShown = false
                    editor.markupModel.removeAllHighlighters()
                }
            })

            return editor
        }

        fun createCodeViewer(
            project: Project,
            graphProperty: GraphProperty<String>,
            disposable: Disposable,
            language: Language,
            message: CompletableMessage
        ): CodePartEditorInfo {
            val forceFoldEditorByDefault = message.getRole() === ChatRole.User
            val content = graphProperty.get()

            val createCodeViewerFile: VirtualFile = createCodeViewerFile(language, content)
            val document: Document =
                createCodeViewerFile.findDocument() ?: throw IllegalStateException("Document not found")

            val editor: EditorEx =
                createCodeViewerEditor(project, createCodeViewerFile as LightVirtualFile, document, disposable)

            val toolbarActionGroup = ActionUtil.getActionGroup("AutoDev.ToolWindow.Snippet.Toolbar")
            toolbarActionGroup?.let {
                val jComponent: ActionToolbarImpl =
                    object : ActionToolbarImpl(ActionPlaces.TOOLBAR, toolbarActionGroup, false) {
                        override fun updateUI() {
                            super.updateUI()
                            editor.component.setBorder(JBUI.Borders.empty())
                        }
                    }

                jComponent.setBackground(editor.backgroundColor)
                jComponent.setOpaque(true)
                jComponent.setTargetComponent(editor.contentComponent)
                editor.headerComponent = jComponent
            }

            editor.scrollPane.setBorder(JBUI.Borders.empty())
            editor.component.setBorder(JBUI.Borders.empty())

            val editorFragment = EditorFragment(project, editor, message)
            editorFragment.setCollapsed(forceFoldEditorByDefault)
            editorFragment.updateExpandCollapseLabel()

            return CodePartEditorInfo(graphProperty, editorFragment.getContent(), editor, createCodeViewerFile)
        }
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    val ref: Ref.ObjectRef<Document?> = Ref.ObjectRef()
    runReadAction {
        val instance = FileDocumentManager.getInstance()
        ref.element = instance.getDocument(this)
    }

    return ref.element
}