package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.diff.DiffStreamHandler
import cc.unitmesh.devti.sketch.ui.LangSketch
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.diff.editor.DiffVirtualFileBase
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.ApplyPatchStatus
import com.intellij.openapi.diff.impl.patch.PatchLine
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
import java.awt.BorderLayout
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
    private var patchActionPanel: JPanel? = null
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
                    patchActionPanel?.background = JBColor(DarculaColors.BLUE, DarculaColors.BLUE)
                }

                override fun mouseExited(e: MouseEvent) {
                    patchActionPanel?.background = JBColor.PanelBackground
                }
            })
        }

        // read content, count add and remove line by '+' and '-' in patch.hunks
        val addLine = patch.hunks.sumOf {
            it.lines.count { it.type == PatchLine.Type.ADD }
        }
        val addLabel = JBLabel("+$addLine").apply {
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            foreground = JBColor(0x00FF00, 0x00FF00)
        }

        val removeLine = patch.hunks.sumOf {
            it.lines.count { it.type == PatchLine.Type.REMOVE }
        }
        val removeLabel = JBLabel("-$removeLine").apply {
            border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            foreground = JBColor(0xFF0000, 0xFF0000)
        }

        val filePanel: JPanel
        if (patch.beforeFileName != null) {
            filePanel = JPanel(BorderLayout()).apply {
                add(filepathLabel, BorderLayout.WEST)
                add(addLabel, BorderLayout.CENTER)
                add(removeLabel, BorderLayout.EAST)
            }
        } else {
            filePanel = JPanel(BorderLayout()).apply {
                add(filepathLabel, BorderLayout.WEST)
            }
        }

        val actionPanel = JPanel(HorizontalLayout(4)).apply {
            isOpaque = true
            actions.forEach { button ->
                add(button)
            }
        }

        patchActionPanel = JPanel(BorderLayout()).apply {
            add(filePanel, BorderLayout.WEST)
            add(actionPanel, BorderLayout.EAST)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        val fileContainer = JPanel(BorderLayout(10, 10)).also {
            it.add(patchActionPanel)
        }
        contentPanel.add(fileContainer, BorderLayout.CENTER)

        mainPanel.add(myHeaderPanel)
        mainPanel.add(contentPanel)
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
                    this@SingleFileDiffView.viewDiffAction()
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
