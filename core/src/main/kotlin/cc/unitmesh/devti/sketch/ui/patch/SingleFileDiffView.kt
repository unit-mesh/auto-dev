package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.diff.DiffStreamHandler
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestProducer
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.editor.DiffEditorTabFilesManager
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.ApplyPatchStatus
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class SingleFileDiffView(
    private val myProject: Project,
    private val currentFile: VirtualFile,
    val patch: TextFilePatch,
    val viewDiffAction: () -> Unit
) : LangSketch {
    private val mainPanel: JPanel = JPanel(VerticalLayout(5))
    private val myHeaderPanel: JPanel = JPanel(BorderLayout())
    private var filePanel: JPanel? = null
    var diffFile: ChainDiffVirtualFile? = null
    private val appliedPatch = GenericPatchApplier.apply(currentFile.readText(), patch.hunks)
    private val oldCode = currentFile.readText()

    init {
        val contentPanel = JPanel(BorderLayout())
        val actions = createActionButtons()
        val filepathLabel = JBLabel(currentFile.name).apply {
            icon = currentFile.fileType.icon
            border = BorderFactory.createEmptyBorder(2, 10, 2, 10)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    FileEditorManager.getInstance(myProject).openFile(currentFile, true)
                }

                override fun mouseEntered(e: MouseEvent) {
                    foreground = JBColor.WHITE
                    filePanel?.background = JBColor(DarculaColors.BLUE, DarculaColors.BLUE)
                }

                override fun mouseExited(e: MouseEvent) {
                    foreground = JBColor.BLACK
                    filePanel?.background = JBColor.PanelBackground
                }
            })
        }

        val actionPanel = JPanel(HorizontalLayout(4)).apply {
            isOpaque = true
            actions.forEach { button ->
                add(button)
            }
        }

        filePanel = JPanel(BorderLayout()).apply {
            add(filepathLabel, BorderLayout.WEST)
            add(actionPanel, BorderLayout.EAST)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        val fileContainer = JPanel(BorderLayout(10, 10)).also {
            it.add(filePanel)
        }
        contentPanel.add(fileContainer, BorderLayout.CENTER)

        mainPanel.add(myHeaderPanel)
        mainPanel.add(contentPanel)
    }

    private fun showDiff() {
        viewDiffAction()
//        if (diffFile != null) {
//            showDiffFile(diffFile!!)
//            return true
//        }
//
//        val document = FileDocumentManager.getInstance().getDocument(currentFile) ?: return false
//        val appliedPatch = GenericPatchApplier.apply(document.text, patch.hunks)
//            ?: return false
//
//        val newText = appliedPatch.patchedText
//        val diffFactory = DiffContentFactoryEx.getInstanceEx()
//        val currentDocContent = diffFactory.create(myProject, currentFile)
//        val newDocContent = diffFactory.create(newText)
//
//        val diffRequest =
//            SimpleDiffRequest(
//                "Shire Diff - ${patch.beforeFileName}",
//                currentDocContent,
//                newDocContent,
//                "Original",
//                "AI generated"
//            )
//
//        val producer = SimpleDiffRequestProducer.create(currentFile.path) {
//            diffRequest
//        }
//
//        val chain = SimpleDiffRequestChain.fromProducer(producer)
//        runInEdt {
//            diffFile = ChainDiffVirtualFile(chain, "Diff")
//            showDiffFile(diffFile!!)
//        }
//
//        return true
    }

    private val diffEditorTabFilesManager = DiffEditorTabFilesManager.getInstance(myProject)

    private fun showDiffFile(diffFile: ChainDiffVirtualFile) {
        diffEditorTabFilesManager.showDiffFile(diffFile, true)
    }

    private fun createActionButtons(): List<JButton> {
        val undoManager = UndoManager.getInstance(myProject)
        val fileEditor = FileEditorManager.getInstance(myProject).getSelectedEditor(currentFile)

        val rollback = JButton("Undo").apply {
            icon = AllIcons.Actions.Rollback
            toolTipText = AutoDevBundle.message("sketch.patch.action.rollback.tooltip")
            isEnabled = undoManager.isUndoAvailable(fileEditor)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (undoManager.isUndoAvailable(fileEditor)) {
                        undoManager.undo(fileEditor)
                    }
                }
            })
        }

        val viewDiffButton = JButton("View").apply {
            icon = AllIcons.Actions.ListChanges
            toolTipText = AutoDevBundle.message("sketch.patch.action.viewDiff.tooltip")

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    showDiff()
                }
            })
        }

        val runStreamButton = JButton("Apply").apply {
            icon = AllIcons.Actions.RunAll
            toolTipText = AutoDevBundle.message("sketch.patch.action.runDiff.tooltip")
            isEnabled = appliedPatch?.status == ApplyPatchStatus.SUCCESS

            addActionListener {
                val document = FileDocumentManager.getInstance().getDocument(currentFile)
                if (document == null) {
                    logger<SingleFileDiffView>().error("Document is null for file: ${currentFile.path}")
                    return@addActionListener
                }

                CommandProcessor.getInstance().executeCommand(myProject, {
                    WriteCommandAction.runWriteCommandAction(myProject) {
                        document.setText(appliedPatch!!.patchedText)

                        if (currentFile is DiffVirtualFileBase) {
                            FileEditorManager.getInstance(myProject).closeFile(currentFile)
                        }
                    }
                }, "ApplyPatch", null)
            }
        }

        val repairButton = JButton("Repair").apply {
            icon = AllIcons.Toolwindows.ToolWindowBuild
            toolTipText = AutoDevBundle.message("sketch.patch.action.repairDiff.tooltip")
            isEnabled = appliedPatch?.status != ApplyPatchStatus.SUCCESS
            foreground = if (isEnabled) JBColor(0xFF0000, 0xFF0000) else JPanel().background

            addActionListener {
                FileEditorManager.getInstance(myProject).openFile(currentFile, true)
                val editor = FileEditorManager.getInstance(myProject).selectedTextEditor ?: return@addActionListener

                val failurePatch = if (patch.hunks.size > 1) {
                    patch.hunks.joinToString("\n") { it.text }
                } else {
                    patch.singleHunkPatchText
                }

                applyDiffRepairSuggestion(myProject, editor, oldCode, failurePatch)
            }
        }

        return listOf(rollback, viewDiffButton, runStreamButton, repairButton)
    }

    private fun showStreamDiff() {
        FileEditorManager.getInstance(myProject).openFile(currentFile, true)
        val editor = FileEditorManager.getInstance(myProject).selectedTextEditor ?: return
        val newText = appliedPatch!!.patchedText

        val diffStreamHandler = DiffStreamHandler(
            myProject,
            editor = editor,
            0,
            oldCode.lines().size,
            onClose = {
            },
            onFinish = {

            })

        diffStreamHandler.normalDiff(oldCode, newText)
    }


    override fun getViewText(): String = currentFile.readText()

    override fun updateViewText(text: String) {}

    override fun getComponent(): JComponent = mainPanel

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}
}

fun applyDiffRepairSuggestion(project: Project, editor: Editor, oldCode: String, patchedCode: String) {
    val diffStreamHandler = DiffStreamHandler(
        project,
        editor = editor,
        0,
        oldCode.lines().size,
        onClose = {
        },
        onFinish = {

        })

    val templateRender = TemplateRender(GENIUS_CODE)
    val template = templateRender.getTemplate("repair-diff.vm")

    templateRender.context = DiffRepairContext(oldCode, patchedCode)
    val prompt = templateRender.renderTemplate(template)

    diffStreamHandler.streamDiffLinesToEditor(oldCode, prompt)
}

data class DiffRepairContext(
    val oldCode: String,
    val patchedCode: String,
) : TemplateContext

fun VirtualFile.readText(): String {
    return VfsUtilCore.loadText(this)
}
