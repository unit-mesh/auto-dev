package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.util.DirUtil
import cc.unitmesh.devti.util.isFile
import cc.unitmesh.devti.util.relativePath
import com.intellij.diff.DiffContentFactoryEx
import com.intellij.diff.DiffContext
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.SystemIndependent
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class PlannerResultSummary(
    private val project: Project,
    private var changes: List<Change>
) : JPanel(BorderLayout()) {
    private val changesPanel = JPanel(GridLayout(0, 1, 0, 1))
    private val statsLabel = JBLabel(AutoDevBundle.message("planner.stats.changes.empty"))
    private val rollbackWorker = RollbackWorker(project)
    private val discardAllButton = HyperlinkLabel(AutoDevBundle.message("planner.action.discard.all")).apply {
        icon = AllIcons.Actions.Cancel
        addHyperlinkListener(object : HyperlinkListener {
            override fun hyperlinkUpdate(e: HyperlinkEvent) {
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    globalActionListener?.onDiscardAll()
                }
            }
        })
    }
    private val acceptAllButton = HyperlinkLabel(AutoDevBundle.message("planner.action.accept.all")).apply {
        icon = AllIcons.Actions.Commit
        addHyperlinkListener(object : HyperlinkListener {
            override fun hyperlinkUpdate(e: HyperlinkEvent) {
                if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    globalActionListener?.onAcceptAll()
                }
            }
        })
    }

    interface ChangeActionListener {
        fun onView(change: Change)
        fun onDiscard(change: Change)
        fun onAccept(change: Change)
    }

    interface GlobalActionListener {
        fun onDiscardAll()
        fun onAcceptAll()
    }

    private var globalActionListener: GlobalActionListener? = object : GlobalActionListener {
        override fun onDiscardAll() {
            try {
                rollbackWorker.doRollback(changes, true)
            } catch (e: Exception) {
                logger<PlannerResultSummary>().warn("Failed to discard all changes: ${e.message}")
            } finally {
                updateChanges(mutableListOf())
            }
        }

        override fun onAcceptAll() {
            changes.forEach { change ->
                changeActionListener.onAccept(change)
            }
        }
    }

    private var changeActionListener: ChangeActionListener = object : ChangeActionListener {
        override fun onView(change: Change) {
            showDiffView(change)
        }

        override fun onDiscard(change: Change) {
            try {
                rollbackWorker.doRollback(listOf(change), false)
            } catch (e: Exception) {
                logger<PlannerResultSummary>().warn("Failed to discard change: ${e.message}")
            }

            val newChanges = changes.toMutableList()
            newChanges.remove(change)
            updateChanges(newChanges)
        }

        override fun onAccept(change: Change) {
            val file = change.afterRevision?.file?.virtualFile
            val content = change.afterRevision?.content ?: change.beforeRevision?.content

            runWriteAction {
                when {
                    file?.isFile == true && content != null -> {
                        val document = FileDocumentManager.getInstance().getDocument(file)
                        if (document != null) {
                            document.setText(content)
                        } else {
                            createNewFile(change, content)
                        }
                    }

                    content != null -> {
                        createNewFile(change, content)
                    }
                }
            }
        }
    }

    private fun createNewFile(change: Change, content: String) {
        val afterFile = calculateNewPath(change)
        if (afterFile == null) {
            val message = AutoDevBundle.message("planner.error.no.after.file")
            AutoDevNotifications.warn(project, message)
            return
        }

        val fileName = afterFile.substringAfterLast('/')
        val parentDir = afterFile.substringBeforeLast('/')
        val dir = DirUtil.getOrCreateDirectory(project.baseDir, parentDir)
        val newFile = dir.createChildData(this, fileName)

        newFile.setBinaryContent(content.toByteArray())
        FileEditorManager.getInstance(project).openFile(newFile, true)
    }

    private fun showDiffView(change: Change) {
        val diffViewer = createViewer(change)

        val dialog = object : DialogWrapper(project) {
            init {
                init()
                title = "Diff Viewer"
                setOKButtonText("Apply")
            }

            override fun createCenterPanel(): JComponent = diffViewer
            override fun doOKAction() {
                super.doOKAction()
                changeActionListener.onAccept(change)
            }

            override fun doCancelAction() {
                super.doCancelAction()
            }
        }

        dialog.show()
    }

    private fun createViewer(change: Change): JComponent {
        when {
            change.type == Change.Type.NEW -> {
                val diffRequest = runWriteAction { createOneSideDiffRequest(change) }
                val diffViewer = SimpleOnesideDiffViewer(object : DiffContext() {
                    override fun getProject() = this@PlannerResultSummary.project
                    override fun isWindowFocused() = false
                    override fun isFocusedInWindow() = false
                    override fun requestFocusInWindow() = Unit
                }, diffRequest)
                diffViewer.init()
                return diffViewer.component
            }
            else -> {
                val diffRequest = runWriteAction { createTwoSideDiffRequest(change) }
                val diffViewer = SimpleDiffViewer(object : DiffContext() {
                    override fun getProject() = this@PlannerResultSummary.project
                    override fun isWindowFocused() = false
                    override fun isFocusedInWindow() = false
                    override fun requestFocusInWindow() = Unit
                }, diffRequest)
                diffViewer.init()
                return diffViewer.component
            }
        }
    }

    private fun createOneSideDiffRequest(change: Change): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val newCode = change.afterRevision?.content ?: ""
        val newDocContent = diffFactory.create(newCode)
        return SimpleDiffRequest("Diff", newDocContent, newDocContent, "AI suggestion", "AI suggestion")
    }

    private fun createTwoSideDiffRequest(change: Change): SimpleDiffRequest {
        val diffFactory = DiffContentFactoryEx.getInstanceEx()
        val oldCode = change.beforeRevision?.content ?: ""
        val newCode = try {
            change.afterRevision?.content ?: ""
        } catch (e: Exception) {
            "Error: ${e.message}"
        }

        val currentDocContent = diffFactory.create(project, oldCode)
        val newDocContent = diffFactory.create(newCode)

        val diffRequest =
            SimpleDiffRequest("Diff", currentDocContent, newDocContent, "Original", "AI suggestion")
        return diffRequest
    }

    init {
        border = JBUI.Borders.customLine(UIUtil.getBoundsColor(), 1, 0, 0, 0)

        val titlePanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(UIUtil.getBoundsColor(), 0, 0, 1, 0),
                JBUI.Borders.empty(4)
            )

            val titleLabelPanel = JPanel(BorderLayout()).apply {
                add(JBLabel(AutoDevBundle.message("planner.change.list.title")).apply {
                    foreground = UIUtil.getLabelForeground()
                    font = JBUI.Fonts.label().asBold()
                }, BorderLayout.WEST)
                add(statsLabel, BorderLayout.EAST)
            }

            val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                add(discardAllButton)
                add(acceptAllButton)
            }

            add(titleLabelPanel, BorderLayout.WEST)
            add(actionsPanel, BorderLayout.EAST)
        }

        add(titlePanel, BorderLayout.NORTH)

        changesPanel.isOpaque = false
        changesPanel.border = JBUI.Borders.empty(1)

        val scrollPane = JBScrollPane(changesPanel).apply {
            border = JBUI.Borders.empty()
            background = background
            viewport.background = background
        }

        add(scrollPane, BorderLayout.CENTER)
        updateChanges(changes.toMutableList())
    }

    fun updateChanges(changes: MutableList<Change>) {
        this.changes = changes
        changesPanel.removeAll()

        if (changes.isEmpty()) {
            statsLabel.text = " - " + AutoDevBundle.message("planner.stats.no.changes")
            changesPanel.add(JBLabel(AutoDevBundle.message("planner.no.code.changes")).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                border = JBUI.Borders.empty(8)
            })

            discardAllButton.isVisible = false
            acceptAllButton.isVisible = false
        } else {
            statsLabel.text = " - " + AutoDevBundle.message("planner.stats.changes.count", changes.size)
            changes.forEach { change ->
                val filePath = change.virtualFile?.relativePath(project)
                    ?: calculateNewPath(change) ?: "Unknown"
                val fileName = filePath.substringAfterLast('/')

                val changePanel = createChangeItemPanel(change, fileName, filePath)
                changesPanel.add(changePanel)
            }

            discardAllButton.isVisible = true
            acceptAllButton.isVisible = true
        }

        changesPanel.revalidate()
        changesPanel.repaint()

        isVisible = true
        revalidate()
        repaint()
    }

    private fun calculateNewPath(change: Change): @NlsSafe @SystemIndependent String? {
        val path = change.afterRevision?.file?.path ?: return null
        val baseDir = project.basePath ?: return path
        val relativePath = path.substringAfter(baseDir)
        return relativePath.trimStart('/')
    }

    private fun createChangeItemPanel(change: Change, fileName: String, filePath: String): JPanel {
        return JPanel().apply {
            isOpaque = true
            background = UIUtil.getListBackground()
            border = JBUI.Borders.empty(4, 8)
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            val changeIcon = when (change.type) {
                Change.Type.NEW -> AutoDevIcons.GIT_NEW
                Change.Type.DELETED -> AutoDevIcons.GIT_DELETE
                Change.Type.MOVED -> AutoDevIcons.GIT_MOVE
                else -> AutoDevIcons.GIT_EDIT
            }

            val fileLabel = HyperlinkLabel(fileName).apply {
                icon = changeIcon
                toolTipText = filePath
                addHyperlinkListener(object : HyperlinkListener {
                    override fun hyperlinkUpdate(e: HyperlinkEvent) {
                        if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                            if (change.type != Change.Type.NEW) {
                                change.virtualFile?.also {
                                    FileEditorManager.getInstance(project).openFile(it, true)
                                }
                            } else {
                                changeActionListener.onView(change)
                            }
                        }
                    }
                })
            }

            add(fileLabel)
            add(Box.createHorizontalStrut(2))

            val pathLabel = JBLabel(filePath).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                toolTipText = filePath
                isOpaque = false
                componentStyle = UIUtil.ComponentStyle.SMALL
                putClientProperty("JComponent.truncateText", true)
                putClientProperty("truncateAtWord", false)
                putClientProperty("html.disable", true)
                minimumSize = JBUI.size(20, preferredSize.height)
                preferredSize = JBUI.size(100, preferredSize.height)
                maximumSize = JBUI.size(Int.MAX_VALUE, preferredSize.height)
            }

            add(pathLabel)
            add(Box.createHorizontalGlue())

            val actionsPanel = JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                val viewButton = createActionButton(
                    AutoDevIcons.VIEW,
                    AutoDevBundle.message("planner.action.view.changes")
                ) { changeActionListener.onView(change) }

                val acceptButton = createActionButton(
                    AutoDevIcons.RUN,
                    AutoDevBundle.message("planner.action.accept.changes")
                ) { changeActionListener.onAccept(change) }

                val discardButton = createActionButton(
                    AllIcons.Actions.Cancel,
                    AutoDevBundle.message("planner.action.discard.changes")
                ) { changeActionListener.onDiscard(change) }

                add(acceptButton)
                add(Box.createHorizontalStrut(2))
                add(viewButton)
                add(Box.createHorizontalStrut(2))
                add(discardButton)
            }

            add(actionsPanel)
        }
    }

    private fun createActionButton(
        icon: Icon,
        tooltip: String,
        action: () -> Unit
    ): JComponent {
        val anAction = object : AnAction(tooltip, tooltip, icon) {
            override fun actionPerformed(e: AnActionEvent) {
                action()
            }
        }
        return KeyboardAccessibleActionButton(anAction)
    }
}

