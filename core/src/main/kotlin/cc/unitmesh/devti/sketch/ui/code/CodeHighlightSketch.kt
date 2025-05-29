package cc.unitmesh.devti.sketch.ui.code

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.gui.snippet.AutoDevRunAction
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

open class CodeHighlightSketch(
    open val project: Project,
    open val text: String,
    private var ideaLanguage: Language? = null,
    val editorLineThreshold: Int = 6,
    val fileName: String? = null,
    val withLeftRightBorder: Boolean = true,
    val showToolbar: Boolean = true
) : JBPanel<CodeHighlightSketch>(VerticalLayout(2)), DataProvider, LangSketch, Disposable {
    private val minDevinLineThreshold = 1
    private var isDevIns = false
    private var devInsCollapsedPanel: JPanel? = null
    private var devInsExpandedPanel: JPanel? = null
    private var isCollapsed = false

    private var textLanguage: String? = if (ideaLanguage != null) ideaLanguage?.displayName else null

    var editorFragment: EditorFragment? = null
    var previewEditor: FileEditor? = null
    private var hasSetupAction = false

    init {
        if (text.isNotNullOrEmpty() && (ideaLanguage?.displayName != "Markdown" && ideaLanguage != PlainTextLanguage.INSTANCE)) {
            initEditor(text, fileName)
        }
    }

    private fun String?.isNotNullOrEmpty(): Boolean {
        return this != null && this.trim().isNotEmpty()
    }

    private var toolbar: ActionToolbar? = null

    fun initEditor(text: String, fileName: String? = null) {
        if (hasSetupAction) return
        hasSetupAction = true

        val editor = EditorUtil.createCodeViewerEditor(project, text, ideaLanguage, fileName, this)

        border = if (withLeftRightBorder) {
            JBEmptyBorder(4, 4, 4, 4)
        } else {
            JBEmptyBorder(4, 0, 0, 0)
        }

        editor.component.isOpaque = true

        if (ideaLanguage?.displayName == "DevIn") {
            isDevIns = true
            editorFragment = EditorFragment(editor, minDevinLineThreshold, previewEditor)
            setupDevInsView(text)
        } else {
            editorFragment = EditorFragment(editor, editorLineThreshold, previewEditor)
            add(editorFragment!!.getContent())
        }

        val isDeclarePackageFile = BuildSystemProvider.isDeclarePackageFile(fileName)
        val lowercase = textLanguage?.lowercase()
        if (textLanguage != null && lowercase != "markdown" && lowercase != "plain text") {
            if (showToolbar && !isDevIns) {
                toolbar = setupActionBar(project, editor, isDeclarePackageFile)
            }
        } else {
            editorFragment?.editor?.backgroundColor = JBColor.PanelBackground
        }

        if (lowercase == "devin") {
            editorFragment?.editor?.setBorder(JBEmptyBorder(1, 1, 0, 1))
        } else if(lowercase != "markdown") {
            editorFragment?.editor?.setBorder(JBEmptyBorder(1, 0, 0, 0))
        }
    }

    private fun setupDevInsView(text: String) {
        devInsCollapsedPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)

            val runAction = ActionManager.getInstance().getAction("AutoDev.ToolWindow.Snippet.RunDevIns") as? AutoDevRunAction
                ?: AutoDevRunAction()
            val runButton = ActionButton(
                runAction,
                runAction.templatePresentation.clone(),
                "AutoDevToolbar",
                JBUI.size(22, 22)
            )

            val firstLine = text.lines().firstOrNull() ?: ""
            val previewLabel = JBLabel(firstLine).apply {
                border = JBUI.Borders.emptyLeft(4)
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        toggleEditorVisibility()
                    }
                })
            }

            val expandCollapseIcon = JBLabel(AllIcons.General.ArrowRight).apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        toggleEditorVisibility()
                    }
                })
            }

            val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
                add(runButton)
            }

            val rightPanel = JPanel(BorderLayout()).apply {
                add(previewLabel, BorderLayout.CENTER)
                add(expandCollapseIcon, BorderLayout.EAST)
            }

            add(leftPanel, BorderLayout.WEST)
            add(rightPanel, BorderLayout.CENTER)
        }

        devInsExpandedPanel = JPanel(VerticalLayout(0)).apply {
            add(editorFragment!!.getContent())
            
            val fewerLinesLabel = createFewerLinesLabel()
            add(fewerLinesLabel)
        }

        add(devInsCollapsedPanel!!)
        isCollapsed = true
    }

    private fun toggleEditorVisibility() {
        if (isCollapsed) {
            remove(devInsCollapsedPanel)
            add(devInsExpandedPanel!!)
            isCollapsed = false
        } else {
            remove(devInsExpandedPanel)
            add(devInsCollapsedPanel!!)
            isCollapsed = true
        }

        revalidate()
        repaint()
    }

    private fun createFewerLinesLabel(): JBLabel {
        return JBLabel("Fewer lines", AllIcons.General.ChevronUp, JBLabel.LEFT).apply {
            border = JBUI.Borders.empty(4, 8)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            isOpaque = true
            background = JBColor.PanelBackground
            
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (!isCollapsed) {
                        toggleEditorVisibility()
                    }
                }
            })
        }
    }

    override fun getViewText(): String {
        return editorFragment?.editor?.document?.text ?: ""
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {
        if (originLanguage == "devin") {
            ideaLanguage = Language.findLanguageByID("DevIn")
            textLanguage = "devin"
        } else if (ideaLanguage == null || ideaLanguage == PlainTextLanguage.INSTANCE) {
            ideaLanguage = language
            textLanguage = originLanguage
        }
    }

    override fun updateViewText(text: String, complete: Boolean) {
        if (!hasSetupAction && text.trim().isNotEmpty()) {
            initEditor(text)
        }

        WriteCommandAction.runWriteCommandAction(project) {
            if (editorFragment?.editor?.isDisposed == true) return@runWriteCommandAction

            val document = editorFragment?.editor?.document
            val normalizedText = StringUtil.convertLineSeparators(text)
            try {
                document?.replaceString(0, document.textLength, normalizedText)

                if (isDevIns && devInsCollapsedPanel != null) {
                    val firstLine = normalizedText.lines().firstOrNull() ?: ""
                    val components = devInsCollapsedPanel!!.components
                    for (comp in components) {
                        if (comp is JPanel && comp.layout is BorderLayout) {
                            val centerComp = (comp.layout as BorderLayout).getLayoutComponent(BorderLayout.CENTER)
                            if (centerComp is JBLabel) {
                                centerComp.text = firstLine
                                break
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                logger<CodeHighlightSketch>().error("Error updating editor text", e)
            }

            val lineCount = document?.lineCount ?: 0
            if (lineCount > editorLineThreshold) {
                editorFragment?.updateExpandCollapseLabel()
            }

            if (complete && isDevIns) {
                if (!isCollapsed) {
                    toggleEditorVisibility()
                }
            }
        }
    }

    override fun onDoneStream(allText: String) {
        if (ideaLanguage?.displayName != "DevIn") return

        val currentText = getViewText()
        if (currentText.startsWith("/" + BuiltinCommand.WRITE.commandName + ":")) {
            val fileName = currentText.lines().firstOrNull()?.substringAfter(":")
            processWriteCommand(currentText, fileName)
            if (BuildSystemProvider.isDeclarePackageFile(fileName)) {
                val ext = fileName?.substringAfterLast(".")
                val parse = CodeFence.parse(editorFragment!!.editor.document.text)
                val language = if (ext != null) CodeFence.findLanguage(ext) else ideaLanguage
                val sketch = CodeHighlightSketch(project, parse.text, language, editorLineThreshold, fileName)
                add(sketch)
            }
        }

        editorFragment?.updateExpandCollapseLabel()
    }

    override fun getComponent(): JComponent = this

    private var hasSetupRenderView = false

    override fun hasRenderView(): Boolean {
        return hasSetupRenderView
    }

    override fun addOrUpdateRenderView(component: JComponent) {
        if (hasSetupRenderView) {
            return
        } else {
            add(component)
            hasSetupRenderView = true
        }

        revalidate()
        repaint()
    }

    override fun getData(dataId: String): Any? = null

    override fun dispose() {
        editorFragment?.editor?.let {
            EditorFactory.getInstance().releaseEditor(it)
        }
        Disposer.dispose(this)
    }
}

/**
 * Add Write Command Action
 */
private fun CodeHighlightSketch.processWriteCommand(currentText: String, fileName: String?) {
    val button = JButton(AutoDevBundle.message("sketch.write.to.file"), AllIcons.Actions.MenuSaveall).apply {
        preferredSize = JBUI.size(120, 30)

        addActionListener {
            val newFileName = "DevIn-${System.currentTimeMillis()}.devin"
            val language = Language.findLanguageByID("DevIn")
            val file = ScratchRootType.getInstance()
                .createScratchFile(project, newFileName, language, currentText)

            this.text = "Written to $fileName"
            this.isEnabled = false

            if (file == null) return@addActionListener

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

    add(panel)
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}