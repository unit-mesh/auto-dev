package cc.unitmesh.devti.sketch.patch

import cc.unitmesh.devti.AutoDevBundle
import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestProducer
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.editor.DiffEditorTabFilesManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.panel
import cc.unitmesh.devti.sketch.LangSketch
import com.intellij.openapi.vfs.VfsUtilCore
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


class SingleFileDiffView(
    private val myProject: Project,
    private val virtualFile: VirtualFile,
    val patch: TextFilePatch,
) : LangSketch {
    private val mainPanel: JPanel = JPanel(VerticalLayout(5))
    private val myHeaderPanel: JPanel = JPanel(BorderLayout())
    private var filePanel: DialogPanel? = null
    var diffFile: ChainDiffVirtualFile? = null

    init {
        val contentPanel = JPanel(BorderLayout())
        val actions = createActionButtons()
        val filepathLabel = JBLabel(virtualFile.name).apply {
            icon = virtualFile.fileType.icon
            border = BorderFactory.createEmptyBorder(2, 10, 2, 10)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val isShowDiffSuccess = showDiff()
                    if (isShowDiffSuccess) return

                    FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
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

        filePanel = panel {
            row {
                cell(filepathLabel).align(AlignX.FILL).resizableColumn()
                actions.forEachIndexed { index, action ->
                    cell(action).align(AlignX.LEFT)
                    if (index < actions.size - 1) {
                        this@panel.gap(RightGap.SMALL)
                    }
                }
            }
        }.apply {
            background = JBColor.PanelBackground
        }

        val fileContainer = JPanel(BorderLayout(10, 10)).also {
            it.add(filePanel)
        }
        contentPanel.add(fileContainer, BorderLayout.CENTER)

        mainPanel.add(myHeaderPanel)
        mainPanel.add(contentPanel)
    }

    private fun showDiff(): Boolean {
        if (diffFile != null) {
            showDiffFile(diffFile!!)
            return true
        }

        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return false
        val appliedPatch = GenericPatchApplier.apply(document.text, patch.hunks)
            ?: return false

        val newText = appliedPatch.patchedText
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val currentDocContent = diffFactory.create(myProject, virtualFile)
        val newDocContent = diffFactory.create(newText)

        val diffRequest =
            SimpleDiffRequest(
                "Shire Diff - ${patch.beforeFileName}",
                currentDocContent,
                newDocContent,
                "Original",
                "AI generated"
            )

        val producer = SimpleDiffRequestProducer.create(virtualFile.path) {
            diffRequest
        }

        val chain = SimpleDiffRequestChain.fromProducer(producer)
        runInEdt {
            diffFile = ChainDiffVirtualFile(chain, "Diff")
            showDiffFile(diffFile!!)
        }

        return true
    }

    private val diffEditorTabFilesManager = DiffEditorTabFilesManager.getInstance(myProject)

    private fun showDiffFile(diffFile: ChainDiffVirtualFile) {
        diffEditorTabFilesManager.showDiffFile(diffFile, true)
    }

    private fun createActionButtons(): List<JButton> {
        val undoManager = UndoManager.getInstance(myProject)
        val fileEditor = FileEditorManager.getInstance(myProject).getSelectedEditor(virtualFile)

        val rollback = JButton(AllIcons.Actions.Rollback).apply {
            toolTipText = AutoDevBundle.message("sketch.patch.action.rollback.tooltip")
            isEnabled = undoManager.isUndoAvailable(fileEditor)
            border = null
            isFocusPainted = false
            isContentAreaFilled = false

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if (undoManager.isUndoAvailable(fileEditor)) {
                        undoManager.undo(fileEditor)
                    }
                }
            })
        }

        return listOf(rollback)
    }

    override fun getViewText(): String = virtualFile.readText()

    override fun updateViewText(text: String) {}

    override fun getComponent(): JComponent = mainPanel

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {}

    fun openDiffView() {
        showDiff()
    }
}

fun VirtualFile.readText(): String {
    return VfsUtilCore.loadText(this)
}
