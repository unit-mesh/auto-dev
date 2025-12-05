package cc.unitmesh.devti.sketch.ui.code

import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.gui.chat.ui.AutoInputService
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.code.processor.EditFileCommandProcessor
import cc.unitmesh.devti.sketch.ui.code.processor.WriteCommandProcessor
import cc.unitmesh.devti.util.parser.CodeFence
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
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
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

open class CodeHighlightSketch(
    open val project: Project,
    open val text: String,
    private var ideaLanguage: Language? = null,
    val editorLineThreshold: Int = 6,
    val fileName: String? = null,
    val withLeftRightBorder: Boolean = true,
    val showToolbar: Boolean = true,
    val isUser: Boolean = false,
) : JBPanel<CodeHighlightSketch>(VerticalLayout(2)), DataProvider, LangSketch, Disposable {
    private val minDevinLineThreshold = 1
    private var isDevIns = false
    private var collapsedPanel: JPanel? = null
    private var previewLabel: JBLabel? = null // 存储预览标签的引用
    private var isCollapsed = true
    private var actionButton: ActionButton? = null
    private var isComplete = isUser
    private var hasProcessedDevInCommands = false

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

    private fun shouldUseCollapsedView(): Boolean {
        val displayName = ideaLanguage?.displayName
        return when {
            displayName == "Markdown" -> false
            ideaLanguage == PlainTextLanguage.INSTANCE -> false
            displayName == plainText -> false
            !isUser && !isDevIns -> false
            else -> true
        }
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
        } else {
            editorFragment = EditorFragment(editor, editorLineThreshold, previewEditor)
        }

        val needsCollapsedView = shouldUseCollapsedView()
        if (needsCollapsedView) {
            setupCollapsedView(text)
        } else {
            add(editorFragment!!.getContent())
            isCollapsed = false
        }

        setupToolbarAndStyling(fileName, editor)
    }


    private val plainText = PlainTextLanguage.INSTANCE.displayName

    private val devinLanguageId = "devin"

    private fun setupToolbarAndStyling(fileName: String?, editor: EditorEx) {
        val isPackageFile = BuildSystemProvider.isDeclarePackageFile(fileName)
        val lowercase = textLanguage?.lowercase()

        if (textLanguage != null && lowercase != "markdown" && textLanguage != plainText) {
            if (showToolbar && lowercase != devinLanguageId) {
                val isShowBottomBorder = collapsedPanel != null
                toolbar = setupActionBar(project, editor, isPackageFile, isShowBottomBorder)
            }
        } else {
            editorFragment?.editor?.backgroundColor = JBColor.PanelBackground
        }

        when (lowercase) {
            devinLanguageId -> editorFragment?.editor?.setBorder(JBEmptyBorder(1, 1, 0, 1))
            "markdown" -> { /* no border changes needed */
            }

            else -> editorFragment?.editor?.setBorder(JBEmptyBorder(1, 0, 0, 0))
        }
    }

    private fun setupCollapsedView(text: String) {
        collapsedPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(2)
            actionButton = if (isDevIns) {
                createDevInsButton(text)
            } else {
                createGenericButton()
            }

            val firstLine = text.lines().firstOrNull() ?: ""
            previewLabel = JBLabel(firstLine).apply {
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
                add(actionButton!!)
            }

            val rightPanel = JPanel(BorderLayout()).apply {
                add(previewLabel!!, BorderLayout.CENTER)
                add(expandCollapseIcon, BorderLayout.EAST)
            }

            add(leftPanel, BorderLayout.WEST)
            add(rightPanel, BorderLayout.CENTER)
        }

        add(collapsedPanel!!)
        isCollapsed = true
        updateActionButtonIcon()
    }

    private var actionButtonPresentation = Presentation()

    private fun createDevInsButton(newText: String): ActionButton {
        val commandIcon = getDevInsCommandIcon(newText)
        actionButtonPresentation = Presentation()
        actionButtonPresentation.icon = commandIcon
        return ActionButton(
            DumbAwareAction.create {
                if (isComplete) return@create
                val sketchService = project.getService(AutoSketchMode::class.java)
                if (sketchService.listener == null) {
                    AutoInputService.getInstance(project).putText(newText)
                } else {
                    sketchService.send(newText)
                }
            },
            actionButtonPresentation,
            "AutoDevToolbar",
            JBUI.size(24, 24)
        )
    }

    private fun createGenericButton(): ActionButton {
        actionButtonPresentation = Presentation()
        actionButtonPresentation.icon = AllIcons.General.ArrowRight
        return ActionButton(
            DumbAwareAction.create {
            },
            actionButtonPresentation,
            "AutoDevToolbar",
            JBUI.size(24, 24)
        )
    }

    private fun getDevInsCommandIcon(text: String): Icon {
        val firstLine = text.lines().firstOrNull() ?: ""
        if (firstLine.startsWith("/")) {
            val commandName = firstLine.substring(1).split(":").firstOrNull()?.trim()
            if (commandName != null) {
                val command = BuiltinCommand.entries.find { it.commandName == commandName }
                if (command != null) {
                    return command.icon
                }
            }
        }
        return AutoDevIcons.RUN // 默认图标
    }

    private fun updateActionButtonIcon() {
        actionButton?.let { button: ActionButton ->
            if (isDevIns) {
                val icon = if (isComplete) {
                    getDevInsCommandIcon(getViewText())
                } else {
                    AutoDevIcons.LOADING
                }
                actionButtonPresentation.setIcon(icon)
            }
            button.repaint()
        }
    }

    private fun toggleEditorVisibility() {
        if (isCollapsed) {
            remove(collapsedPanel)
            add(editorFragment!!.getContent())

            val lineCount = editorFragment?.editor?.document?.lineCount ?: 0
            if (lineCount > editorLineThreshold) {
                editorFragment?.updateExpandCollapseLabel()
            } else {
                val fewerLinesLabel = createFewerLinesLabel()
                add(fewerLinesLabel)
            }

            isCollapsed = false
        } else {
            removeAll()
            add(collapsedPanel!!)
            isCollapsed = true
        }

        revalidate()
        repaint()
    }

    private fun createFewerLinesLabel(): JBLabel {
        return JBLabel("Hidden", AllIcons.General.ChevronUp, JBLabel.LEFT).apply {
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
        if (originLanguage == devinLanguageId) {
            ideaLanguage = Language.findLanguageByID("DevIn")
            textLanguage = devinLanguageId
        } else if (ideaLanguage == null || ideaLanguage == PlainTextLanguage.INSTANCE) {
            ideaLanguage = language
            textLanguage = originLanguage
        }
    }

    override fun updateViewText(text: String, complete: Boolean) {
        isComplete = complete

        if (!hasSetupAction && text.trim().isNotEmpty()) {
            initEditor(text)
        }

        if (hasProcessedDevInCommands) {
            return
        }

        WriteCommandAction.runWriteCommandAction(project) {
            if (editorFragment?.editor?.isDisposed == true) return@runWriteCommandAction

            val document = editorFragment?.editor?.document
            val normalizedText = StringUtil.convertLineSeparators(text)
            try {
                document?.replaceString(0, document.textLength, normalizedText)
                if (previewLabel != null && shouldUseCollapsedView()) {
                    val firstLine = normalizedText.lines().firstOrNull() ?: ""
                    previewLabel!!.text = firstLine
                }
            } catch (e: Throwable) {
                logger<CodeHighlightSketch>().error("Error updating editor text", e)
            }

            updateActionButtonIcon()

            val lineCount = document?.lineCount ?: 0
            if (lineCount > editorLineThreshold) {
                editorFragment?.updateExpandCollapseLabel()
            }

            if (complete && !isCollapsed && shouldUseCollapsedView()) {
                toggleEditorVisibility()
            }
        }

        if (complete && !isUser && ideaLanguage?.displayName == "DevIn") {
            processDevInCommands(text)
            hasProcessedDevInCommands = true
        }
    }

    private fun processDevInCommands(currentText: String) {
        if (currentText.startsWith("/" + BuiltinCommand.WRITE.commandName + ":")) {
            val fileName = currentText.lines().firstOrNull()?.substringAfter(":")
            val writeProcessor = WriteCommandProcessor(project)
            val panel = writeProcessor.processWriteCommand(currentText, fileName)
            add(panel)

            if (BuildSystemProvider.isDeclarePackageFile(fileName)) {
                val ext = fileName?.substringAfterLast(".")
                val parse = CodeFence.parse(editorFragment!!.editor.document.text)
                val language = if (ext != null) CodeFence.findLanguage(ext) else ideaLanguage
                val sketch = CodeHighlightSketch(project, parse.text, language, editorLineThreshold, fileName)
                add(sketch)
            }
        } else if (currentText.startsWith("/" + BuiltinCommand.EDIT_FILE.commandName)) {
            val editProcessor = EditFileCommandProcessor(project)
            val panel = editProcessor.processEditFileCommand(currentText) { diffSketch ->
                add(diffSketch.getComponent())
            }
            add(panel)
        }
    }

    override fun onDoneStream(allText: String) {
        // Logic moved to updateViewText with isComplete condition for better real-time performance
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
            try {
                EditorFactory.getInstance().releaseEditor(it)
            } catch (e: Exception) {
                /// ignore
            }
        }
        Disposer.dispose(this)
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}