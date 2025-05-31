package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.AutoDevIcons
import cc.unitmesh.devti.history.ChatHistoryService
import cc.unitmesh.devti.history.ChatSessionHistory
import cc.unitmesh.devti.llms.custom.Message
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class ViewHistoryAction : AnAction(
    AutoDevBundle.message("action.view.history.text"),
    AutoDevBundle.message("action.view.history.description"),
    AutoDevIcons.HISTORY
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365
        
        return when {
            years > 0 -> "${years}年前"
            months > 0 -> "${months}个月前"
            weeks > 0 -> "${weeks}周前"
            days > 0 -> "${days}天前"
            hours > 0 -> "${hours}小时前"
            minutes > 0 -> "${minutes}分钟前"
            else -> "刚刚"
        }
    }

    private inner class SessionListItem(
        val session: ChatSessionHistory,
        val relativeTime: String
    )

    private inner class SessionListCellRenderer(
        private val project: Project,
        private val onDelete: (ChatSessionHistory) -> Unit
    ) : ColoredListCellRenderer<SessionListItem>() {
        
        override fun customizeCellRenderer(
            list: javax.swing.JList<out SessionListItem>,
            value: SessionListItem?,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ) {
            if (value == null) return
            
            // 创建主面板
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(4, 8)
            
            // 左侧标题
            val titleLabel = JLabel(value.session.name)
            titleLabel.font = titleLabel.font.deriveFont(14f)
            
            // 右侧时间和删除按钮的面板
            val rightPanel = JPanel(BorderLayout())
            
            // 时间标签
            val timeLabel = JLabel(value.relativeTime)
            timeLabel.font = timeLabel.font.deriveFont(12f)
            timeLabel.foreground = if (selected) list.selectionForeground else list.foreground.darker()
            
            // 删除按钮
            val deleteButton = JButton(AllIcons.Actions.Close)
            deleteButton.isOpaque = false
            deleteButton.isBorderPainted = false
            deleteButton.preferredSize = JBUI.size(16, 16)
            deleteButton.addActionListener {
                onDelete(value.session)
            }
            
            rightPanel.add(timeLabel, BorderLayout.CENTER)
            rightPanel.add(deleteButton, BorderLayout.EAST)
            
            panel.add(titleLabel, BorderLayout.WEST)
            panel.add(rightPanel, BorderLayout.EAST)
            
            // 设置简单的文本渲染
            append(value.session.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            append(" - ${value.relativeTime}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyService = project.getService(ChatHistoryService::class.java)
        var sessions = historyService.getAllSessions().sortedByDescending { it.createdAt }

        if (sessions.isEmpty()) {
            return
        }

        fun createAndShowPopup() {
            val listItems = sessions.map { SessionListItem(it, formatRelativeTime(it.createdAt)) }
            val jbList = JBList(listItems)
            jbList.selectionMode = ListSelectionModel.SINGLE_SELECTION
            
            val onDeleteSession = { session: ChatSessionHistory ->
                val result = Messages.showYesNoDialog(
                    project,
                    "确定要删除会话 \"${session.name}\" 吗？",
                    "删除会话",
                    Messages.getQuestionIcon()
                )
                
                if (result == Messages.YES) {
                    historyService.deleteSession(session.id)
                    sessions = historyService.getAllSessions().sortedByDescending { it.createdAt }
                    // 关闭当前popup并重新创建
                    createAndShowPopup()
                }
            }
            
            jbList.cellRenderer = SessionListCellRenderer(project, onDeleteSession)
            
            // 添加右键菜单支持
            jbList.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.button == MouseEvent.BUTTON3) { // 右键
                        val index = jbList.locationToIndex(e.point)
                        if (index >= 0) {
                            jbList.selectedIndex = index
                            val selectedSession = listItems[index].session
                            onDeleteSession(selectedSession)
                        }
                    }
                }
            })

            JBPopupFactory.getInstance()
                .createListPopupBuilder(jbList)
                .setTitle(AutoDevBundle.message("popup.title.session.history"))
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .setItemChoosenCallback {
                    val selectedIndex = jbList.selectedIndex
                    if (selectedIndex != -1) {
                        val selectedSession = listItems[selectedIndex].session
                        loadSessionIntoSketch(project, selectedSession)
                    }
                }
                .createPopup()
                .showInBestPositionFor(e.dataContext)
        }
        
        createAndShowPopup()
    }

    private fun loadSessionIntoSketch(project: Project, session: ChatSessionHistory) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("AutoDev") ?: return
        val contentManager = toolWindow.contentManager
        val sketchPanel = contentManager.contents.firstNotNullOfOrNull { it.component as? SketchToolWindow }

        sketchPanel?.let {
            it.resetSketchSession()

            val agentStateService = project.getService(AgentStateService::class.java)
            agentStateService.resetMessages()
            session.messages.forEach { msg ->
                agentStateService.addMessage(Message(msg.role, msg.content))
            }

            it.displayMessages(session.messages)
            session.messages.firstOrNull { msg -> msg.role == "user" }?.content?.let { intention ->
                 agentStateService.state = agentStateService.state.copy(originIntention = intention)
            }

            toolWindow.activate(null)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }
}