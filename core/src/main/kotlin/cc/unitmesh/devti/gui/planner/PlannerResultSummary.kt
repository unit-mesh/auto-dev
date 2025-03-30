package cc.unitmesh.devti.gui.planner

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.util.relativePath
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
                val discardAllButton = HyperlinkLabel(AutoDevBundle.message("planner.action.discard.all")).apply {
                    icon = AllIcons.Actions.Cancel
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                globalActionListener?.onDiscardAll()
                            }
                        }
                    })
                }

                val acceptAllButton = HyperlinkLabel(AutoDevBundle.message("planner.action.accept.all")).apply {
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
            statsLabel.text = " - " + AutoDevBundle.message("planner.stats.no.changes")
            changesPanel.add(JBLabel(AutoDevBundle.message("planner.no.code.changes")).apply {
                foreground = UIUtil.getLabelDisabledForeground()
                border = JBUI.Borders.empty(8)
            })
        } else {
            statsLabel.text = " - " + AutoDevBundle.message("planner.stats.changes.count", changes.size)
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
        return JPanel().apply {
            isOpaque = true
            background = UIUtil.getListBackground()
            border = JBUI.Borders.empty(4, 8)
            layout = BoxLayout(this, BoxLayout.X_AXIS)

            val changeIcon = when (change.type) {
                Change.Type.NEW -> AllIcons.Actions.New
                Change.Type.DELETED -> AllIcons.Actions.GC
                Change.Type.MOVED -> AllIcons.Actions.Forward
                else -> AllIcons.Actions.Edit
            }

            val fileLabel = HyperlinkLabel(fileName).apply {
                icon = changeIcon
                toolTipText = filePath
                addHyperlinkListener(object : HyperlinkListener {
                    override fun hyperlinkUpdate(e: HyperlinkEvent) {
                        if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                            changeActionListener.onView(change)
                        }
                    }
                })
            }

            add(fileLabel)
            add(Box.createHorizontalStrut(4))

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
            add(Box.createHorizontalGlue()) // This pushes the action buttons to the right

            val actionsPanel = JPanel().apply {
                isOpaque = false
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                val viewButton = createActionButton(
                    AllIcons.Actions.Preview,
                    AutoDevBundle.message("planner.action.view.changes")
                ) { changeActionListener.onView(change) }

                val discardButton = createActionButton(
                    AllIcons.Actions.Cancel,
                    AutoDevBundle.message("planner.action.discard.changes")
                ) { changeActionListener.onDiscard(change) }

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

