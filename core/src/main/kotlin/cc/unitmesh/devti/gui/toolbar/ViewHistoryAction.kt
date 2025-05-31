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
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

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
    ) : ListCellRenderer<SessionListItem> {
        
        override fun getListCellRendererComponent(
            list: javax.swing.JList<out SessionListItem>,
            value: SessionListItem,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ): Component {
            // 创建主面板，使用 BorderLayout
            val panel = JPanel(BorderLayout(10, 0))
            panel.border = JBUI.Borders.empty(4, 8)

            // 设置背景颜色
            if (selected) {
                panel.background = list.selectionBackground
                panel.foreground = list.selectionForeground
            } else {
                panel.background = list.background
                panel.foreground = list.foreground
            }
            panel.isOpaque = true

            val sessionName = value.session.name
            val maxLength = 30  // 根据UI宽度调整最大长度
            val displayName = if (sessionName.length > maxLength) {
                sessionName.substring(0, maxLength - 3) + "..."
            } else {
                sessionName
            }
            
            val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            contentPanel.isOpaque = false

            // 会话名称
            val titleLabel = JLabel(displayName)
            titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 12f)
            titleLabel.toolTipText = sessionName // 完整名称作为工具提示
            
            // 时间标签
            val timeLabel = JLabel(" - ${value.relativeTime}")
            timeLabel.font = timeLabel.font.deriveFont(11f)
            
            if (selected) {
                titleLabel.foreground = list.selectionForeground
                timeLabel.foreground = list.selectionForeground.darker()
            } else {
                titleLabel.foreground = list.foreground
                timeLabel.foreground = list.foreground.darker()
            }

            contentPanel.add(titleLabel)
            contentPanel.add(timeLabel)

            val deleteButton = JLabel(AllIcons.Actions.Close)
            deleteButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            deleteButton.border = JBUI.Borders.emptyLeft(8)
            deleteButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    e.consume() // 防止事件传播到列表
                    onDelete(value.session)
                }
                
                override fun mouseEntered(e: MouseEvent) {
                    deleteButton.icon = AllIcons.Actions.CloseHovered
                }
                
                override fun mouseExited(e: MouseEvent) {
                    deleteButton.icon = AllIcons.Actions.Close
                }
            })

            panel.add(contentPanel, BorderLayout.CENTER)
            panel.add(deleteButton, BorderLayout.EAST)

            panel.preferredSize = Dimension(panel.preferredSize.width, 35)
            return panel
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
            jbList.fixedCellHeight = 35
            
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
                    createAndShowPopup()
                }
            }

            jbList.cellRenderer = SessionListCellRenderer(project, onDeleteSession)

            val scrollPane = JBScrollPane(jbList)
            scrollPane.border = null

            // 设置 Popup 的固定宽度和自适应高度
            val popupWidth = 400
            val maxPopupHeight = 400
            val itemHeight = 35  // 与fixedCellHeight一致
            val calculatedHeight = (sessions.size * itemHeight + 20).coerceAtMost(maxPopupHeight)
            
            scrollPane.preferredSize = Dimension(popupWidth, calculatedHeight)

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

            val popupBuilder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, jbList)
                .setTitle(AutoDevBundle.message("popup.title.session.history"))
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setCancelOnOtherWindowOpen(true)

            val popup: JBPopup = popupBuilder.createPopup()

            // 设置 Popup 的最小尺寸
            popup.setMinimumSize(Dimension(300, 150))

            jbList.addListSelectionListener {
                if (!it.valueIsAdjusting && jbList.selectedIndex != -1) {
                    val selectedIndex = jbList.selectedIndex
                    popup.closeOk(null)
                    val selectedSession = listItems[selectedIndex].session
                    loadSessionIntoSketch(project, selectedSession)
                }
            }

            popup.showInBestPositionFor(e.dataContext)
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