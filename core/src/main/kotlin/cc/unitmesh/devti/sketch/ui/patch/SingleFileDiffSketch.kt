package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevColors
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.settings.coder.coderSetting
import cc.unitmesh.devti.sketch.lint.SketchCodeInspection
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.template.context.TemplateContext
import cc.unitmesh.devti.util.isFile
import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.DiffContext
import com.intellij.diff.contents.EmptyContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.*
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.LocalTimeCounter
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.nio.charset.Charset
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class SingleFileDiffSketch(
    private val myProject: Project,
    private var currentFile: VirtualFile,
    var patch: TextFilePatch,
    val viewDiffAction: () -> Unit
) : LangSketch {
    private val mainPanel: JPanel = JPanel(VerticalLayout(0))
    private val myHeaderPanel: JPanel = JPanel(BorderLayout())
    
    private val patchProcessor = PatchProcessor(myProject)
    private var patchActionPanel: JPanel? = null
    private val oldCode = if (currentFile.isFile && currentFile.exists()) {
        try {
            currentFile.readText()
        } catch (e: IOException) {
            logger<SingleFileDiffSketch>().warn("Failed to read file: ${currentFile.path}", e)
            ""
        }
    } else ""

    private var appliedPatch = patchProcessor.applyPatch(oldCode, patch)

    private val actionPanel = JPanel(HorizontalLayout(4)).apply {
        isOpaque = true
    }

    private var newCode = appliedPatch?.patchedText ?: ""
    private val isAutoRepair = myProject.coderSetting.state.enableAutoRepairDiff

    init {
        val contentPanel = JPanel(BorderLayout())
        val actions = createActionButtons(
            this@SingleFileDiffSketch.currentFile,
            this@SingleFileDiffSketch.appliedPatch,
            this@SingleFileDiffSketch.patch
        )

        val fileName = if (currentFile.name.contains("/")) {
            currentFile.name.substringAfterLast("/")
        } else {
            currentFile.name
        }

        val filepathLabel = JBLabel(fileName).apply {
            icon = currentFile.fileType.icon
            border = BorderFactory.createEmptyBorder(2, 4, 2, 4)

            val originalColor = foreground
            val hoverColor = if (currentFile !is LightVirtualFile) {
                AutoDevColors.FILE_HOVER_COLOR
            } else {
                foreground
            }

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (currentFile is LightVirtualFile) return
                    FileEditorManager.getInstance(myProject).openFile(currentFile, true)
                }

                override fun mouseEntered(e: MouseEvent?) {
                    if (currentFile is LightVirtualFile) return

                    foreground = hoverColor
                    cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, hoverColor),
                        BorderFactory.createEmptyBorder(2, 4, 1, 4)
                    )
                }

                override fun mouseExited(e: MouseEvent?) {
                    if (currentFile is LightVirtualFile) return

                    foreground = originalColor
                    cursor = java.awt.Cursor.getDefaultCursor()
                    border = BorderFactory.createEmptyBorder(2, 4, 2, 4)
                }
            })
        }

        val addLine = patch.hunks.sumOf {
            it.lines.count { it.type == PatchLine.Type.ADD }
        }
        val addLabel = JBLabel("+$addLine").apply {
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            foreground = AutoDevColors.ADD_LINE_COLOR // Extracted from inline JBColor definition
        }

        val removeLine = patch.hunks.sumOf {
            it.lines.count { it.type == PatchLine.Type.REMOVE }
        }
        val removeLabel = JBLabel("-$removeLine").apply {
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            foreground = AutoDevColors.REMOVE_LINE_COLOR // Extracted from inline JBColor definition
        }

        val filePanel: JPanel = if (patch.beforeFileName != null) {
            JPanel(BorderLayout()).apply {
                add(filepathLabel, BorderLayout.WEST)
                add(addLabel, BorderLayout.CENTER)
                add(removeLabel, BorderLayout.EAST)
            }
        } else {
            JPanel(BorderLayout()).apply {
                add(filepathLabel, BorderLayout.WEST)
            }
        }

        actions.forEach { button ->
            actionPanel.add(button)
        }

        patchActionPanel = JPanel(BorderLayout()).apply {
            add(filePanel, BorderLayout.WEST)
            add(actionPanel, BorderLayout.EAST)
            border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
        }

        val fileContainer = JPanel(BorderLayout(8, 8)).also {
            it.add(patchActionPanel)
        }
        contentPanel.add(fileContainer, BorderLayout.CENTER)
        mainPanel.add(myHeaderPanel)
        mainPanel.add(contentPanel)

        if (myProject.coderSetting.state.enableDiffViewer && appliedPatch?.status == ApplyPatchStatus.SUCCESS) {
            patchProcessor.registerPatchChange(patch)

            invokeLater {
                val diffPanel = createDiffViewer(oldCode, newCode)
                mainPanel.add(diffPanel)
            }
        }
    }

    private fun createDiffViewer(oldCode: String, newCode: String): JComponent {
        val component: JComponent = if (oldCode == "") {
            val diffRequest = runWriteAction { createOneSideDiffRequest(newCode) }
            val diffViewer = SimpleOnesideDiffViewer(object : DiffContext() {
                override fun getProject() = myProject
                override fun isWindowFocused() = false
                override fun isFocusedInWindow() = false
                override fun requestFocusInWindow() = Unit
            }, diffRequest)
            diffViewer.init()
            diffViewer.component
        } else {
            val diffRequest = runWriteAction { createTwoSideDiffRequest(oldCode, newCode) }
            val diffViewer = SimpleDiffViewer(object : DiffContext() {
                override fun getProject() = myProject
                override fun isWindowFocused() = false
                override fun isFocusedInWindow() = false
                override fun requestFocusInWindow() = Unit
            }, diffRequest)
            diffViewer.init()
            diffViewer.component
        }

        val wrapperPanel = JPanel(BorderLayout())
        wrapperPanel.add(component, BorderLayout.CENTER)

        val minHeight = (mainPanel.height * 0.25).toInt()

        wrapperPanel.preferredSize = Dimension(wrapperPanel.preferredSize.width, maxOf(200, minHeight))
        wrapperPanel.maximumSize = Dimension(Int.MAX_VALUE, maxOf(200, minHeight))

        return wrapperPanel
    }

    private fun createTwoSideDiffRequest(oldCode: String, newCode: String): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val currentDocContent = diffFactory.create(myProject, oldCode)
        val newDocContent = diffFactory.create(newCode)

        val diffRequest =
            SimpleDiffRequest("Diff", currentDocContent, newDocContent, AutoDevBundle.message("sketch.diff.original"), AutoDevBundle.message("sketch.diff.aiSuggestion"))
        return diffRequest
    }

    private fun createOneSideDiffRequest(newCode: String): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val newDocContent = diffFactory.create(newCode)

        val diffRequest =
            SimpleDiffRequest("Diff", EmptyContent(), newDocContent, "", AutoDevBundle.message("sketch.diff.aiSuggestion"))
        return diffRequest
    }

    private fun createActionButtons(
        file: VirtualFile,
        patch: GenericPatchApplier.AppliedPatch?,
        filePatch: TextFilePatch,
        isRepaired: Boolean = false
    ): List<JButton> {
        val viewButton = JButton(AutoDevBundle.message("sketch.patch.view")).apply {
            icon = AutoDevIcons.VIEW
            toolTipText = AutoDevBundle.message("sketch.patch.action.viewDiff.tooltip")

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    this@SingleFileDiffSketch.viewDiffAction()
                }
            })
        }

        val applyButton = JButton(AutoDevBundle.message("sketch.patch.apply")).apply {
            icon = AutoDevIcons.RUN
            toolTipText = AutoDevBundle.message("sketch.patch.action.applyDiff.tooltip")
            isEnabled = !patchProcessor.isFailure(patch)

            addActionListener {
                patchProcessor.applyPatchToFile(file, patch)
            }
        }

        val text = if (isRepaired) {
            AutoDevBundle.message("sketch.patch.repaired")
        } else {
            AutoDevBundle.message("sketch.patch.repair")
        }
        val repairButton = JButton(text).apply {
            val isFailedPatch = patchProcessor.isFailure(patch)
            isEnabled = isFailedPatch
            icon = if (isAutoRepair && isFailedPatch) {
                AutoDevIcons.LOADING
            } else {
                AutoDevIcons.REPAIR
            }

            toolTipText = AutoDevBundle.message("sketch.patch.action.repairDiff.tooltip")
            foreground = if (isEnabled) AutoDevColors.REMOVE_LINE_COLOR else JPanel().background // Replacing inline JBColor

            addActionListener {
                FileEditorManager.getInstance(myProject).openFile(file, true)
                val editor = FileEditorManager.getInstance(myProject).selectedTextEditor ?: return@addActionListener

                if (myProject.coderSetting.state.enableDiffViewer) {
                    icon = AutoDevIcons.LOADING
                    patchProcessor.performAutoRepair(oldCode, filePatch) { repairedPatch, fixedCode ->
                        icon = AutoDevIcons.REPAIR
                        newCode = fixedCode
                        updatePatchPanel(repairedPatch, fixedCode) {
                            // do nothing
                        }

                        runInEdt {
                            createDiffViewer(oldCode, fixedCode).let { diffViewer ->
                                mainPanel.add(diffViewer)
                                mainPanel.revalidate()
                                mainPanel.repaint()
                            }
                        }
                    }
                } else {
                    val failurePatch = if (filePatch.hunks.size > 1) {
                        filePatch.hunks.joinToString("\n") { it.text }
                    } else {
                        filePatch.singleHunkPatchText
                    }
                    DiffRepair.applyDiffRepairSuggestion(myProject, editor, oldCode, failurePatch)
                }
            }
        }

        return listOf(viewButton, applyButton, repairButton)
    }

    override fun getViewText(): String = currentFile.readText()

    override fun updateViewText(text: String, complete: Boolean) {}

    override fun getComponent(): JComponent = mainPanel

    private var isRepaired = false
    override fun onComplete(code: String) {
        if (isRepaired) return
        if (isAutoRepair && appliedPatch?.status != ApplyPatchStatus.SUCCESS) {
            executeAutoRepair {
                runAutoLint(currentFile)
            }
        } else {
            runAutoLint(currentFile)
        }

        isRepaired = true
    }

    fun runAutoLint(file: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            val task = object : Task.Backgroundable(myProject, "Analysis code style", false) {
                override fun run(indicator: ProgressIndicator) {
                    lintCheckForNewCode(file)
                }
            }

            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }

    fun lintCheckForNewCode(currentFile: VirtualFile) {
        if (newCode.isEmpty()) return
        val newFile = LightVirtualFile(currentFile, newCode, LocalTimeCounter.currentTime())

        val psiFile = runReadAction { PsiManager.getInstance(myProject).findFile(newFile) } ?: return
        val errors = SketchCodeInspection.runInspections(myProject, psiFile, currentFile, HighlightSeverity.ERROR)
        if (errors.isNotEmpty()) {
            SketchCodeInspection.showErrors(errors, this@SingleFileDiffSketch.mainPanel)
        }
    }

    private fun executeAutoRepair(postAction: () -> Unit) {
        patchProcessor.performAutoRepair(oldCode, patch) { repairedPatch, fixedCode ->
            this.patch = repairedPatch
            updatePatchPanel(repairedPatch, fixedCode, postAction)
        }
    }

    private fun updatePatchPanel(patch: TextFilePatch, fixedCode: String, postAction: () -> Unit) {
        appliedPatch = patchProcessor.applyPatch(oldCode, patch)

        runInEdt {
            WriteAction.compute<Unit, Throwable> {
                currentFile.writeText(fixedCode)
            }
        }

        patchProcessor.registerPatchChange(patch)

        createActionButtons(currentFile, appliedPatch, patch, isRepaired = true).let { actions ->
            actionPanel.removeAll()
            actions.forEach { button ->
                actionPanel.add(button)
            }
        }

        postAction()

        mainPanel.revalidate()
        mainPanel.repaint()
    }

    override fun dispose() {}
}

data class DiffRepairContext(
    val intention: String?,
    val patchedCode: String,
    val oldCode: String,
) : TemplateContext

fun VirtualFile.readText(): String {
    return VfsUtilCore.loadText(this)
}

fun createPatchFromCode(oldCode: String, newCode: String): TextFilePatch? {
    val buildPatchHunks: List<PatchHunk> = TextPatchBuilder.buildPatchHunks(oldCode, newCode)
    val textFilePatch = TextFilePatch(Charset.defaultCharset())
    buildPatchHunks.forEach { hunk ->
        textFilePatch.addHunk(hunk)
    }

    return textFilePatch
}


@RequiresWriteLock
fun VirtualFile.writeText(content: String) {
    saveText(this, content)
}

@Throws(IOException::class)
fun saveText(file: VirtualFile, text: String) {
    val charset = file.charset
    runWriteAction {
        file.getOutputStream(file).use { stream ->
            stream.write(text.toByteArray(charset))
        }
    }
}

