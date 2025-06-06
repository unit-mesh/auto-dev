package cc.unitmesh.devti.sketch.ui.code

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.command.dataprovider.BuiltinCommand
import cc.unitmesh.devti.gui.chat.ui.AutoInputService
import cc.unitmesh.devti.provider.BuildSystemProvider
import cc.unitmesh.devti.provider.RunService
import cc.unitmesh.devti.sketch.AutoSketchMode
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.sketch.ui.patch.DiffLangSketchProvider
import cc.unitmesh.devti.util.AutoDevCoroutineScope
import cc.unitmesh.devti.util.parser.CodeFence
import kotlinx.coroutines.launch
import com.intellij.icons.AllIcons
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
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
import javax.swing.Icon

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
    private var isCollapsed = true // 默认折叠状态
    private var actionButton: ActionButton? = null
    private var isComplete = isUser

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

        // 检查是否需要折叠视图
        val needsCollapsedView = shouldUseCollapsedView()
        if (needsCollapsedView) {
            setupCollapsedView(text)
        } else {
            // 直接添加编辑器内容，不使用折叠
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

            // 根据是否为 DevIns 创建不同的按钮
            actionButton = if (isDevIns) {
                createDevInsButton(text)
            } else {
                createGenericButton()
            }

            val firstLine = text.lines().firstOrNull() ?: ""
            // 创建预览标签并存储引用，以便在 updateViewText 中直接更新
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
                // 普通编辑器的按钮行为，可以根据需要扩展
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
            // 展开：移除折叠面板，添加编辑器内容和折叠标签
            remove(collapsedPanel)
            add(editorFragment!!.getContent())

            val fewerLinesLabel = createFewerLinesLabel()
            add(fewerLinesLabel)

            isCollapsed = false
        } else {
            // 折叠：移除所有内容，只显示折叠面板
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

        // Initialize editor if not already done and text is not empty
        if (!hasSetupAction && text.trim().isNotEmpty()) {
            initEditor(text)
        }

        WriteCommandAction.runWriteCommandAction(project) {
            if (editorFragment?.editor?.isDisposed == true) return@runWriteCommandAction

            val document = editorFragment?.editor?.document
            val normalizedText = StringUtil.convertLineSeparators(text)
            try {
                document?.replaceString(0, document.textLength, normalizedText)

                // Update collapsed panel preview text if applicable
                if (previewLabel != null && shouldUseCollapsedView()) {
                    val firstLine = normalizedText.lines().firstOrNull() ?: ""
                    previewLabel!!.text = firstLine
                }
            } catch (e: Throwable) {
                logger<CodeHighlightSketch>().error("Error updating editor text", e)
            }

            // Update action button icon state
            updateActionButtonIcon()

            val lineCount = document?.lineCount ?: 0
            if (lineCount > editorLineThreshold) {
                editorFragment?.updateExpandCollapseLabel()
            }

            // Auto-collapse view when complete (only for collapsible views)
            if (complete && !isCollapsed && shouldUseCollapsedView()) {
                toggleEditorVisibility()
            }
        }
    }

    override fun onDoneStream(allText: String) {
        // Only process DevIns commands for non-user messages
        if (isUser || ideaLanguage?.displayName != "DevIn") return

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
        } else if (currentText.startsWith("/" + BuiltinCommand.EDIT_FILE.commandName)) {
            processEditFileCommand(currentText)
        }
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

/**
 * Add Write Command Action
 */
fun CodeHighlightSketch.processWriteCommand(currentText: String, fileName: String?) {
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

/**
 * Add Edit File Command Action
 */
fun CodeHighlightSketch.processEditFileCommand(currentText: String) {
    val isAutoSketchMode = AutoSketchMode.getInstance(project).isEnable

    val button = if (isAutoSketchMode) {
        JButton("Auto Executing...", AutoDevIcons.LOADING).apply {
            isEnabled = false
            preferredSize = JBUI.size(150, 30)
        }
    } else {
        JButton("Execute Edit File", AllIcons.Actions.Execute).apply {
            preferredSize = JBUI.size(120, 30)
        }
    }

    val panel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(button)
    }
    add(panel)

    val executeCommand = {
        button.isEnabled = false
        button.text = if (isAutoSketchMode) "Auto Executing..." else "Executing..."
        button.icon = AutoDevIcons.LOADING

        AutoDevCoroutineScope.scope(project).launch {
            executeEditFileCommand(project, currentText) { result ->
                runInEdt {
                    handleExecutionResult(result, button)
                }
            }
        }
    }

    if (isAutoSketchMode) {
        executeCommand()
    } else {
        button.addActionListener { executeCommand() }
    }
}

private fun CodeHighlightSketch.handleExecutionResult(result: String?, button: JButton) {
    if (result != null && !result.startsWith("DEVINS_ERROR")) {
        val diffSketch = DiffLangSketchProvider().create(project, result)
        add(diffSketch.getComponent())
        button.text = "Executed"
        button.icon = AllIcons.Actions.Checked
    } else {
        button.text = "Failed"
        button.icon = AllIcons.General.Error
        AutoDevNotifications.warn(project, result ?: "Unknown error occurred")
    }
}

private suspend fun executeEditFileCommand(project: Project, currentText: String, callback: (String?) -> Unit) {
    try {
        val codeFences = CodeFence.parseAll(currentText)

        if (codeFences.isEmpty()) {
            callback("DEVINS_ERROR: No edit_file commands found in content")
            return
        }

        val editFileCommand = cc.unitmesh.devti.command.EditFileCommand(project)
        val results = mutableListOf<String>()

        // Execute each edit_file command
        for (codeFence in codeFences) {
            val editRequest = editFileCommand.parseEditRequest(codeFence.text)
            if (editRequest == null) continue

            val result = editFileCommand.executeEdit(editRequest)
            when (result) {
                is cc.unitmesh.devti.command.EditResult.Success -> {
                    val projectDir = project.guessProjectDir()
                    val targetFile = projectDir?.findFileByRelativePath(editRequest.targetFile)
                    if (targetFile != null) {
                        runInEdt {
                            FileEditorManager.getInstance(project).openFile(targetFile, true)
                        }
                    }
                    results.add(result.message)
                }

                is cc.unitmesh.devti.command.EditResult.Error -> {
                    results.add("DEVINS_ERROR: ${result.message}")
                }
            }
        }

        callback(results.joinToString("\n"))
    } catch (e: Exception) {
        callback("DEVINS_ERROR: ${e.message}")
    }
}

@RequiresReadLock
fun VirtualFile.findDocument(): Document? {
    return ReadAction.compute<Document, Throwable> {
        FileDocumentManager.getInstance().getDocument(this)
    }
}