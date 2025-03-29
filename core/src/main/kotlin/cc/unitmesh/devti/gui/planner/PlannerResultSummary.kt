package cc.unitmesh.devti.gui.planner

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class PlannerResultSummary(
    private val project: Project,
    private var changes: List<Change>
) : JPanel(BorderLayout()) {
    private val changesPanel = JPanel(GridLayout(0, 1, 0, 1))
    private val statsLabel = JBLabel("No changes")
    
    interface ChangeActionListener {
        fun onView(change: Change)
        fun onDiscard(change: Change)
        fun onAccept(change: Change)
        fun onRemove(change: Change)
    }
    
    interface GlobalActionListener {
        fun onDiscardAll()
        fun onAcceptAll()
    }
    
    private var changeActionListener: ChangeActionListener? = null
    private var globalActionListener: GlobalActionListener? = null

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
                val filePath = change.virtualFile?.path ?: "Unknown"
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
            
            val actionsPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
                isOpaque = false
                
                val viewButton = HyperlinkLabel("").apply {
                    icon = AllIcons.Actions.Preview
                    toolTipText = "View changes"
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                changeActionListener?.onView(change)
                            }
                        }
                    })
                }
                
                val discardButton = HyperlinkLabel("").apply {
                    icon = AllIcons.Actions.Cancel
                    toolTipText = "Discard changes"
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                changeActionListener?.onDiscard(change)
                            }
                        }
                    })
                }
                
                val acceptButton = HyperlinkLabel("").apply {
                    icon = AllIcons.Actions.Commit
                    toolTipText = "Accept changes"
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                changeActionListener?.onAccept(change)
                            }
                        }
                    })
                }
                
                val removeButton = HyperlinkLabel("").apply {
                    icon = AllIcons.Actions.GC
                    toolTipText = "Remove from list"
                    addHyperlinkListener(object : HyperlinkListener {
                        override fun hyperlinkUpdate(e: HyperlinkEvent) {
                            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                                changeActionListener?.onRemove(change)
                            }
                        }
                    })
                }
                
                add(viewButton)
                add(discardButton)
                add(acceptButton)
                add(removeButton)
            }
            
            add(infoPanel, BorderLayout.CENTER)
            add(actionsPanel, BorderLayout.EAST)
            
            // Add hover effect
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    background = UIUtil.getListSelectionBackground(false)
                    repaint()
                }
                
                override fun mouseExited(e: MouseEvent) {
                    background = UIUtil.getListBackground()
                    repaint()
                }
            })
        }
    }
    
    // Set action listeners
    fun setChangeActionListener(listener: ChangeActionListener) {
        this.changeActionListener = listener
    }
    
    fun setGlobalActionListener(listener: GlobalActionListener) {
        this.globalActionListener = listener
    }
}
