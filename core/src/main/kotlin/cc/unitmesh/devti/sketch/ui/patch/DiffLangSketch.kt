package cc.unitmesh.devti.sketch.ui.patch

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.util.findFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.changes.patch.AbstractFilePatchInProgress
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.MatchPatchPaths
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.containers.MultiMap
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class DiffLangSketch(private val myProject: Project, private var patchContent: String) : ExtensionLangSketch {
    private val mainPanel: JPanel = JPanel(VerticalLayout(5))
    private val myHeaderPanel: JPanel = JPanel(BorderLayout())
    private val shelfExecutor = ApplyPatchDefaultExecutor(myProject)
    private val myReader: PatchReader? = PatchReader(patchContent).also {
        try {
            it.parseAllPatches()
        } catch (e: Exception) {
            AutoDevNotifications.error(myProject, "Failed to parse patch: ${e.message}")
            null
        }
    }
    private val filePatches: MutableList<TextFilePatch> = try {
        myReader?.textPatches
    } catch (e: Exception) {
        logger<DiffLangSketch>().warn("Failed to parse patch: ${e.message}")
        mutableListOf()
    } ?: mutableListOf()

    init {
        if (filePatches.size > 1 || filePatches.any { it.beforeName == null }) {
            val header = createHeaderAction()
            myHeaderPanel.add(header, BorderLayout.EAST)
            mainPanel.add(myHeaderPanel)
        }

        mainPanel.border = JBUI.Borders.compound(JBUI.Borders.empty(0, 10))

        ApplicationManager.getApplication().invokeLater {
            if (filePatches.isEmpty()) {
                val msg = "PatchProcessor: no valid patches found, please check the patch content"
                AutoDevNotifications.warn(myProject, msg)

                val repairButton = JButton("Repair").apply {
                    icon = AllIcons.Actions.IntentionBulb
                    toolTipText = "Try to repair the patch content"

                    val editor = tryGetEditor()
                    if (editor == null) {
                        AutoDevNotifications.error(myProject, "Failed to get editor")
                        return@apply
                    }

                    addActionListener {
                        this@apply.icon = AutoDevIcons.InProgress
                        DiffRepair.applyDiffRepairSuggestion(myProject, editor, editor.document.text, patchContent)
                    }
                }

                val panel = JPanel()
                panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
                panel.add(repairButton)
                mainPanel.add(panel)

                return@invokeLater
            }

//            try {
            filePatches.forEach { patch ->
                val diffPanel = createDiffPanel(patch)
                if (diffPanel != null) {
                    mainPanel.add(diffPanel.getComponent())
                    mainPanel.revalidate()
                }
            }
//            } catch (e: Exception) {
//                AutoDevNotifications.error(myProject, "Failed to create diff panel: ${e.message}")
//            }
        }
    }

    private fun createDiffPanel(patch: TextFilePatch): SingleFileDiffSketch? {
        return when {
            patch.beforeName != null -> {
                /// if before file is empty, should set new code empty, it should be new file
                val originFile = myProject.findFile(patch.beforeName!!) ?: LightVirtualFile(patch.beforeName!!, "")
                createSingleFileDiffSketch(originFile, patch)
            }

            patch.afterName != null -> {
                val virtualFile = myProject.findFile(patch.afterName!!) ?: LightVirtualFile(patch.afterName!!, "")
                createSingleFileDiffSketch(virtualFile, patch)
            }

            else -> {
                createErrorSketch(patch)
            }
        }
    }

    private fun createErrorSketch(patch: TextFilePatch): SingleFileDiffSketch {
        val fileName = patch.beforeName ?: ""
        val virtualFile = LightVirtualFile(fileName, getChunkText(patch))
        return createSingleFileDiffSketch(virtualFile, patch)
    }

    private fun getChunkText(patch: TextFilePatch): String {
        if (patch.hunks.size > 1) {
            return patch.hunks.joinToString("\n") { it.toString() }
        }

        return patch.singleHunkPatchText
    }

    private fun createSingleFileDiffSketch(virtualFile: VirtualFile, patch: TextFilePatch): SingleFileDiffSketch {
        return SingleFileDiffSketch(myProject, virtualFile, patch, ::handleViewDiffAction).apply {
            this.onComplete("")
        }
    }

    private fun tryGetEditor(): Editor? {
        var defaultEditor = FileEditorManager.getInstance(myProject).selectedTextEditor ?: return null

        val fileRegex = Regex("/patch:(.*)")
        val matchResult = fileRegex.find(patchContent)
        val filePath = matchResult?.groupValues?.get(1) ?: ""
        val virtualFile = myProject.findFile(filePath) ?: return defaultEditor
        val fileEditor = FileEditorManager.getInstance(myProject).getSelectedEditor(virtualFile) as? TextEditor

        return fileEditor?.editor ?: defaultEditor
    }

    private fun createHeaderAction(): JComponent {
        val acceptButton = JButton(AllIcons.Actions.SetDefault).apply {
            toolTipText = AutoDevBundle.message("sketch.patch.action.accept.tooltip")
            addActionListener {
                handleAcceptAction()
            }
        }

        val rejectButton = JButton(AllIcons.Actions.Rollback).apply {
            this.toolTipText = AutoDevBundle.message("sketch.patch.action.reject.tooltip")
            addActionListener {
                handleRejectAction()
            }
        }

        val viewDiffButton = JButton(AllIcons.Actions.ListChanges).apply {
            this.toolTipText = AutoDevBundle.message("sketch.patch.action.viewDiff.tooltip")
            addActionListener {
                handleViewDiffAction()
            }
        }

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
        panel.add(acceptButton)
        panel.add(rejectButton)
        panel.add(viewDiffButton)

        return panel
    }

    private fun handleAcceptAction() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
        val commandProcessor: CommandProcessor = CommandProcessor.getInstance()

        commandProcessor.executeCommand(myProject, {
            commandProcessor.markCurrentCommandAsGlobal(myProject)

            val matchedPatches =
                MatchPatchPaths(myProject).execute(filePatches, true)

            val patchGroups = MultiMap<VirtualFile, AbstractFilePatchInProgress<*>>()
            for (patchInProgress in matchedPatches) {
                patchGroups.putValue(patchInProgress.base, patchInProgress)
            }

            if (filePatches.isEmpty()) {
                AutoDevNotifications.error(myProject, "PatchProcessor: no patches found")
                return@executeCommand
            }

            val pathsFromGroups = ApplyPatchDefaultExecutor.pathsFromGroups(patchGroups)
            val additionalInfo = myReader?.getAdditionalInfo(pathsFromGroups)
            shelfExecutor.apply(filePatches, patchGroups, null, "LlmGen.diff", additionalInfo)
        }, "ApplyPatch", null, UndoConfirmationPolicy.REQUEST_CONFIRMATION, false)
    }

    private fun handleRejectAction() {
        val undoManager = UndoManager.getInstance(myProject)
        val fileEditor = FileEditorManager.getInstance(myProject).selectedEditor ?: return
        if (undoManager.isUndoAvailable(fileEditor)) {
            undoManager.undo(fileEditor)
        }
    }

    fun handleViewDiffAction() {
        val beforeNames = filePatches.mapNotNull { it.beforeName }
        if (beforeNames.size > 1) {
            MyApplyPatchFromClipboardDialog(myProject, patchContent).show()
            return
        } else {
            showSingleDiff(this@DiffLangSketch.myProject, this@DiffLangSketch.patchContent, this) {
                handleAcceptAction()
            }
        }
    }

    override fun getExtensionName(): String = "patch"
    override fun getViewText(): String = patchContent
    override fun updateViewText(text: String, complete: Boolean) {
        this.patchContent = text
    }

    override fun getComponent(): JComponent = mainPanel
    override fun dispose() {}
}

fun showSingleDiff(project: Project, patchContent: String, disposable: Disposable, handleAccept: (() -> Unit)?) {
    val editorProvider = FileEditorProvider.EP_FILE_EDITOR_PROVIDER.extensionList.firstOrNull {
        it.javaClass.simpleName == "DiffPatchFileEditorProvider" || it.javaClass.simpleName == "DiffEditorProvider"
    }

    if (editorProvider != null) {
        val virtualFile = LightVirtualFile("AutoDev-Diff-Lang.diff", patchContent)
        val editor = editorProvider.createEditor(project, virtualFile)

        object : DialogWrapper(project) {
            init {
                title = "Diff Preview"
                setOKButtonText("Accept")
                init()
            }

            override fun doOKAction() {
                handleAccept?.invoke()
                super.doOKAction()
            }

            override fun createCenterPanel(): JComponent {
                return editor.component
            }
        }.show()
    } else {
        MyApplyPatchFromClipboardDialog(project, patchContent).show()
        return
    }
}
