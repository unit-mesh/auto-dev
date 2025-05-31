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
import javax.swing.border.EmptyBorder

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

    // 跟踪正在删除的会话ID，避免冲突
    private var deletingSessionId: String? = null

    private inner class SessionListCellRenderer(
        private val project: Project,
        private val onDelete: (ChatSessionHistory) -> Unit
    ) : ListCellRenderer<SessionListItem> {
        
        override fun getListCellRendererComponent(
            list: JList<out SessionListItem>,
            value: SessionListItem,
            index: Int,
            selected: Boolean,
            hasFocus: Boolean
        ): Component {
            // 创建主面板
            val panel = JPanel(BorderLayout(10, 0))
            panel.border = JBUI.Borders.empty(4, 8)
            panel.name = "CELL_PANEL_$index"  // 添加唯一标识

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
            val maxLength = 30
            val displayName = if (sessionName.length > maxLength) {
                sessionName.substring(0, maxLength - 3) + "..."
            } else {
                sessionName
            }
            
            val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
            contentPanel.isOpaque = false
            contentPanel.name = "CONTENT_PANEL_$index"  // 添加唯一标识

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

            // 删除按钮面板，添加唯一标识
            val deleteButtonPanel = JPanel(BorderLayout())
            deleteButtonPanel.isOpaque = false
            deleteButtonPanel.name = "DELETE_BUTTON_PANEL_$index"

            // 使用按钮代替标签，避免事件冲突
            val deleteButton = JButton()
            deleteButton.name = "DELETE_BUTTON_$index"  // 添加唯一标识
            deleteButton.isOpaque = false
            deleteButton.isBorderPainted = false
            deleteButton.isContentAreaFilled = false
            deleteButton.isFocusPainted = false
            deleteButton.icon = AllIcons.Actions.Close
            deleteButton.rolloverIcon = AllIcons.Actions.CloseHovered
            deleteButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            deleteButton.preferredSize = Dimension(16, 16)
            deleteButton.toolTipText = "删除会话"
            
            // 为删除按钮添加事件标记，便于在列表的鼠标监听器中识别
            deleteButton.putClientProperty("sessionId", value.session.id)
            
            deleteButton.addActionListener { e ->
                e.source?.let { source ->
                    if (source is JButton) {
                        // 将正在删除的会话ID记录下来
                        deletingSessionId = value.session.id
                        onDelete(value.session)
                    }
                }
            }

            deleteButtonPanel.add(deleteButton, BorderLayout.CENTER)

            panel.add(contentPanel, BorderLayout.CENTER)
            panel.add(deleteButtonPanel, BorderLayout.EAST)

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

        var currentPopup: JBPopup? = null
        
        fun createAndShowPopup() {
            currentPopup?.cancel()
            
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
                    deletingSessionId = null  // 重置删除标记
                    createAndShowPopup()
                } else {
                    deletingSessionId = null  // 用户取消删除，重置标记
                }
            }

            jbList.cellRenderer = SessionListCellRenderer(project, onDeleteSession)

            val scrollPane = JBScrollPane(jbList)
            scrollPane.border = null

            // 设置 Popup 的固定宽度和自适应高度
            val popupWidth = 400
            val maxPopupHeight = 400
            val itemHeight = 35
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
                    } else if (e.button == MouseEvent.BUTTON1 && e.clickCount == 1) { // 左键单击
                        val index = jbList.locationToIndex(e.point)
                        if (index >= 0) {
                            // 检查点击位置是否在删除按钮区域
                            val cell = jbList.getCellBounds(index, index)
                            val cellComponent = jbList.cellRenderer.getListCellRendererComponent(
                                jbList, listItems[index], index, false, false
                            )

                            // 在该方法之外，显式检查是否点击了删除按钮
                            val isDeleteButtonClick = isClickOnDeleteButton(e.point, cellComponent, cell)
                            
                            // 如果不是点击删除按钮，或者当前没有正在处理的删除操作，则处理选择逻辑
                            if (!isDeleteButtonClick && deletingSessionId != listItems[index].session.id) {
                                jbList.selectedIndex = index
                                val selectedSession = listItems[index].session
                                currentPopup?.closeOk(null)
                                loadSessionIntoSketch(project, selectedSession)
                            }
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

            currentPopup = popupBuilder.createPopup()
            currentPopup?.setMinimumSize(Dimension(300, 150))
            currentPopup?.showInBestPositionFor(e.dataContext)
        }

        createAndShowPopup()
    }

    // 检查点击是否在删除按钮上
    private fun isClickOnDeleteButton(point: Point, cellComponent: Component, cellBounds: Rectangle): Boolean {
        if (cellComponent is JPanel) {
            val pointInCell = Point(
                point.x - cellBounds.x,
                point.y - cellBounds.y
            )
            
            // 递归查找所有子组件，检查是否点击在DELETE_BUTTON开头的组件上
            return findComponentAtPoint(cellComponent, pointInCell, "DELETE_BUTTON")
        }
        return false
    }
    
    // 递归查找指定前缀名称的组件
    private fun findComponentAtPoint(container: Container, point: Point, namePrefix: String): Boolean {
        // 检查当前容器是否匹配
        if (container.name?.startsWith(namePrefix) == true) {
            return true
        }
        
        // 递归检查所有子组件
        for (i in 0 until container.componentCount) {
            val child = container.getComponent(i)
            if (!child.isVisible || !child.bounds.contains(point)) {
                continue
            }
            
            // 检查子组件名称
            if (child.name?.startsWith(namePrefix) == true) {
                return true
            }
            
            // 递归检查子容器
            if (child is Container) {
                val childPoint = Point(point.x - child.x, point.y - child.y)
                if (findComponentAtPoint(child, childPoint, namePrefix)) {
                    return true
                }
            }
        }
        
        return false
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