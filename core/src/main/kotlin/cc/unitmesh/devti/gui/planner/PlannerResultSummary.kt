package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.util.relativePath
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.RollbackWorker
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class PlannerResultSummary(
    private val project: Project,
    private var changes: List<Change>
) : JPanel(BorderLayout()) {
    private val changesPanel = JPanel(GridLayout(0, 1, 0, 1))
    private val statsLabel = JBLabel("No changes")
    private val rollbackWorker = RollbackWorker(project)

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
            rollbackWorker.doRollback(changes, false)
            updateChanges(mutableListOf())
        }

        override fun onAcceptAll() {

        }
    }


    private var changeActionListener: ChangeActionListener = object : ChangeActionListener {
        override fun onView(change: Change) {
            change.virtualFile?.also {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        }

        override fun onDiscard(change: Change) {
            rollbackWorker.doRollback(listOf(change), false)
            val newChanges = changes.toMutableList()
            newChanges.remove(change)
            updateChanges(newChanges)
        }

        override fun onAccept(change: Change) {}
    }

    init {
        background = JBUI.CurrentTheme.ToolWindow.background()
        border = JBUI.Borders.empty(10)

        val titlePanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(10)

            val titleLabelPanel = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(JBLabel("Change list").apply {
                    foreground = UIUtil.getLabelForeground()
                    font = JBUI.Fonts.label().asBold()
                }, BorderLayout.WEST)
                add(statsLabel, BorderLayout.EAST)
            }

            val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
                isOpaque = false

                val discardAllButton = HyperlinkLabel("Discard all").apply {
                    icon = AllIcons.Actions.Cancel
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                globalActionListener?.onDiscardAll()
                            }
                        }
                    })
                }

                val acceptAllButton = HyperlinkLabel("Accept all").apply {
                    icon = AllIcons.Actions.Commit
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                globalActionListener?.onAcceptAll()
                            }
                        }
                    })
                }

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
            statsLabel.text = " No changes"
            changesPanel.add(JBLabel("No code changes").apply {
                foreground = UIUtil.getLabelDisabledForeground()
                border = JBUI.Borders.empty(10)
            })
        } else {
            statsLabel.text = " (Total ${changes.size} files changed)"
            changes.forEach { change ->
                val filePath = change.virtualFile?.relativePath(project) ?: "Unknown"
                val fileName = filePath.substringAfterLast('/')

                val changePanel = createChangeItemPanel(change, fileName, filePath)
                changesPanel.add(changePanel)
            }
        }

        changesPanel.revalidate()
        changesPanel.repaint()

        isVisible = true
        revalidate()
        repaint()
    }

    private fun createChangeItemPanel(change: Change, fileName: String, filePath: String): JPanel {
        return JPanel(BorderLayout()).apply {
            isOpaque = true
            background = UIUtil.getListBackground()
            border = JBUI.Borders.empty(5, 8)

            val changeIcon = when (change.type) {
                Change.Type.NEW -> AllIcons.Actions.New
                Change.Type.DELETED -> AllIcons.Actions.GC
                Change.Type.MOVED -> AllIcons.Actions.Forward
                else -> AllIcons.Actions.Edit
            }

            val infoPanel = JPanel(BorderLayout()).apply {
                isOpaque = false

                val fileLabel = JBLabel(fileName, changeIcon, JBLabel.LEFT).apply {
                    toolTipText = filePath
                }

                add(fileLabel, BorderLayout.CENTER)
            }

            val pathLabel = JBLabel(filePath).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                toolTipText = filePath
            }

            val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
                isOpaque = false

                val viewButton = JButton().apply {
                    icon = AllIcons.Actions.Preview
                    toolTipText = "View changes"
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isFocusPainted = false
                    margin = JBUI.emptyInsets()
                    preferredSize = JBUI.size(20, 20)
                    addActionListener {
                        changeActionListener?.onView(change)
                    }
                }

                val discardButton = JButton().apply {
                    icon = AllIcons.Actions.Cancel
                    toolTipText = "Discard changes"
                    isBorderPainted = false
                    isContentAreaFilled = false
                    isFocusPainted = false
                    margin = JBUI.emptyInsets()
                    preferredSize = JBUI.size(20, 20)
                    addActionListener {
                        changeActionListener?.onDiscard(change)
                    }
                }

                add(viewButton)
                add(discardButton)
            }

            add(infoPanel, BorderLayout.NORTH)
            add(pathLabel, BorderLayout.CENTER)
            add(actionsPanel, BorderLayout.EAST)
        }
    }
}

