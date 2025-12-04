package cc.unitmesh.devins.idea.toolwindow.changes

import cc.unitmesh.agent.diff.ChangeType
import cc.unitmesh.agent.diff.FileChange
import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.DiffContext
import com.intellij.diff.contents.EmptyContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.TextRevisionNumber
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

private val LOG = logger<IdeaFileChangeDiffDialogWrapper>()

/**
 * Dialog for displaying file change diff using IntelliJ's DialogWrapper.
 * Uses IntelliJ's SimpleDiffViewer for proper diff rendering and RollbackWorker for revert.
 */
class IdeaFileChangeDiffDialogWrapper(
    private val project: Project,
    private val fileChange: FileChange,
    private val onUndoCallback: () -> Unit,
    private val onKeepCallback: () -> Unit,
    private val onDismissCallback: () -> Unit
) : DialogWrapper(project) {

    private val change: Change = FileChangeConverter.toChange(project, fileChange)
    private val rollbackWorker = RollbackWorker(project)

    init {
        title = "Diff: ${fileChange.getFileName()}"
        setOKButtonText("Keep")
        setCancelButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        mainPanel.preferredSize = Dimension(800, 600)
        mainPanel.border = JBUI.Borders.empty(8)

        // Header with file info
        val headerPanel = createHeaderPanel()
        mainPanel.add(headerPanel, BorderLayout.NORTH)

        // Diff viewer
        val diffViewer = createDiffViewer()
        mainPanel.add(diffViewer, BorderLayout.CENTER)

        return mainPanel
    }

    private fun createHeaderPanel(): JPanel {
        val headerPanel = JPanel(BorderLayout())
        headerPanel.border = JBUI.Borders.empty(0, 0, 8, 0)

        val fileIcon = when (fileChange.changeType) {
            ChangeType.CREATE -> AllIcons.General.Add
            ChangeType.EDIT -> AllIcons.Actions.Edit
            ChangeType.DELETE -> AllIcons.General.Remove
            ChangeType.RENAME -> AllIcons.Actions.Edit
        }

        val fileLabel = JBLabel(fileChange.getFileName()).apply {
            icon = fileIcon
            font = JBUI.Fonts.label().asBold()
        }

        val pathLabel = JBLabel(fileChange.filePath).apply {
            foreground = UIUtil.getLabelDisabledForeground()
            font = JBUI.Fonts.smallFont()
        }

        val leftPanel = JPanel(BorderLayout()).apply {
            add(fileLabel, BorderLayout.NORTH)
            add(pathLabel, BorderLayout.SOUTH)
        }

        // Diff stats
        val diffStats = fileChange.getDiffStats()
        val statsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            val addLabel = JBLabel("+${diffStats.addedLines}").apply {
                foreground = JBUI.CurrentTheme.NotificationInfo.foregroundColor()
            }
            val removeLabel = JBLabel("-${diffStats.deletedLines}").apply {
                foreground = JBUI.CurrentTheme.NotificationError.foregroundColor()
            }
            add(addLabel)
            add(removeLabel)
        }

        headerPanel.add(leftPanel, BorderLayout.WEST)
        headerPanel.add(statsPanel, BorderLayout.EAST)

        return headerPanel
    }

    private fun createDiffViewer(): JComponent {
        return when (fileChange.changeType) {
            ChangeType.CREATE -> {
                val diffRequest = createOneSideDiffRequest()
                val diffViewer = SimpleOnesideDiffViewer(object : DiffContext() {
                    override fun getProject() = this@IdeaFileChangeDiffDialogWrapper.project
                    override fun isWindowFocused() = true
                    override fun isFocusedInWindow() = true
                    override fun requestFocusInWindow() = Unit
                }, diffRequest)
                diffViewer.init()
                diffViewer.component
            }
            ChangeType.DELETE -> {
                val diffRequest = createOneSideDiffRequestForDelete()
                val diffViewer = SimpleOnesideDiffViewer(object : DiffContext() {
                    override fun getProject() = this@IdeaFileChangeDiffDialogWrapper.project
                    override fun isWindowFocused() = true
                    override fun isFocusedInWindow() = true
                    override fun requestFocusInWindow() = Unit
                }, diffRequest)
                diffViewer.init()
                diffViewer.component
            }
            else -> {
                val diffRequest = createTwoSideDiffRequest()
                val diffViewer = SimpleDiffViewer(object : DiffContext() {
                    override fun getProject() = this@IdeaFileChangeDiffDialogWrapper.project
                    override fun isWindowFocused() = true
                    override fun isFocusedInWindow() = true
                    override fun requestFocusInWindow() = Unit
                }, diffRequest)
                diffViewer.init()
                diffViewer.component
            }
        }
    }

    private fun createOneSideDiffRequest(): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val newCode = fileChange.newContent ?: ""
        val newDocContent = diffFactory.create(newCode)
        return SimpleDiffRequest("New File", EmptyContent(), newDocContent, "", "New Content")
    }

    private fun createOneSideDiffRequestForDelete(): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val oldCode = fileChange.originalContent ?: ""
        val oldDocContent = diffFactory.create(oldCode)
        return SimpleDiffRequest("Deleted File", oldDocContent, EmptyContent(), "Original Content", "")
    }

    private fun createTwoSideDiffRequest(): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val oldCode = fileChange.originalContent ?: ""
        val newCode = fileChange.newContent ?: ""

        val currentDocContent = diffFactory.create(project, oldCode)
        val newDocContent = diffFactory.create(newCode)

        return SimpleDiffRequest("Diff", currentDocContent, newDocContent, "Original", "Modified")
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0))
        panel.border = JBUI.Borders.empty(8, 0, 0, 0)

        val closeButton = JButton("Close").apply {
            addActionListener {
                onDismissCallback()
                close(CANCEL_EXIT_CODE)
            }
        }

        val undoButton = JButton("Undo").apply {
            icon = AllIcons.Actions.Rollback
            addActionListener {
                performUndo()
            }
        }

        val keepButton = JButton("Keep").apply {
            icon = AllIcons.Actions.Commit
            addActionListener {
                onKeepCallback()
                close(OK_EXIT_CODE)
            }
        }

        panel.add(closeButton)
        panel.add(undoButton)
        panel.add(keepButton)

        return panel
    }

    private fun performUndo() {
        try {
            // Try to use RollbackWorker for proper VCS integration
            rollbackWorker.doRollback(listOf(change), false)
            onUndoCallback()
            close(OK_EXIT_CODE)
        } catch (e: Exception) {
            LOG.warn("RollbackWorker failed, falling back to manual revert", e)
            // Fallback to manual revert
            performManualUndo()
        }
    }

    private fun performManualUndo() {
        runWriteAction {
            try {
                when (fileChange.changeType) {
                    ChangeType.CREATE -> {
                        // For created files, delete or clear the content
                        val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileChange.filePath)
                        virtualFile?.let { vf ->
                            val document = FileDocumentManager.getInstance().getDocument(vf)
                            document?.setText("")
                        }
                    }
                    ChangeType.EDIT, ChangeType.RENAME -> {
                        // Restore original content
                        fileChange.originalContent?.let { original ->
                            val virtualFile = LocalFileSystem.getInstance().findFileByPath(fileChange.filePath)
                            virtualFile?.let { vf ->
                                val document = FileDocumentManager.getInstance().getDocument(vf)
                                document?.setText(original)
                            }
                        }
                    }
                    ChangeType.DELETE -> {
                        // For deleted files, recreate them
                        fileChange.originalContent?.let { original ->
                            val parentPath = fileChange.filePath.substringBeforeLast('/')
                            val fileName = fileChange.filePath.substringAfterLast('/')
                            val parentDir = LocalFileSystem.getInstance().findFileByPath(parentPath)
                            parentDir?.let { dir ->
                                val newFile = dir.createChildData(project, fileName)
                                val document = FileDocumentManager.getInstance().getDocument(newFile)
                                document?.setText(original)
                            }
                        }
                    }
                }
                onUndoCallback()
                close(OK_EXIT_CODE)
            } catch (e: Exception) {
                LOG.error("Failed to undo change for ${fileChange.filePath}", e)
            }
        }
    }

    override fun doCancelAction() {
        onDismissCallback()
        super.doCancelAction()
    }

    companion object {
        fun show(
            project: Project,
            change: FileChange,
            onUndo: () -> Unit,
            onKeep: () -> Unit,
            onDismiss: () -> Unit
        ): Boolean {
            val dialog = IdeaFileChangeDiffDialogWrapper(
                project = project,
                fileChange = change,
                onUndoCallback = onUndo,
                onKeepCallback = onKeep,
                onDismissCallback = onDismiss
            )
            return dialog.showAndGet()
        }
    }
}

/**
 * Utility object to convert FileChange to IntelliJ's Change API.
 * Similar to PatchConverter in core module.
 */
object FileChangeConverter {

    /**
     * Convert a FileChange to IntelliJ's Change object.
     */
    fun toChange(project: Project, fileChange: FileChange): Change {
        val basePath = project.basePath ?: System.getProperty("user.dir")
        val file = File(fileChange.filePath)
        val filePath: FilePath = VcsUtil.getFilePath(file, false)

        val fileStatus = when (fileChange.changeType) {
            ChangeType.CREATE -> FileStatus.ADDED
            ChangeType.DELETE -> FileStatus.DELETED
            ChangeType.EDIT -> FileStatus.MODIFIED
            ChangeType.RENAME -> FileStatus.MODIFIED
        }

        val beforeRevision: ContentRevision? = if (fileStatus != FileStatus.ADDED) {
            object : CurrentContentRevision(filePath) {
                override fun getRevisionNumber(): VcsRevisionNumber =
                    TextRevisionNumber(VcsBundle.message("local.version.title"))

                override fun getContent(): @NonNls String? = fileChange.originalContent
            }
        } else null

        val afterRevision: ContentRevision? = if (fileStatus != FileStatus.DELETED) {
            object : CurrentContentRevision(filePath) {
                override fun getRevisionNumber(): VcsRevisionNumber =
                    TextRevisionNumber(VcsBundle.message("local.version.title"))

                override fun getContent(): @NonNls String? = fileChange.newContent
            }
        } else null

        return Change(beforeRevision, afterRevision, fileStatus)
    }
}
