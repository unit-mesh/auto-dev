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
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBList
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.ListSelectionModel

class ViewHistoryAction : AnAction(
    AutoDevBundle.message("action.view.history.text"),
    AutoDevBundle.message("action.view.history.description"),
    AutoDevIcons.HISTORY
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val historyService = project.getService(ChatHistoryService::class.java)
        val sessions = historyService.getAllSessions().sortedByDescending { it.createdAt }

        if (sessions.isEmpty()) {
            // Optionally show a message if no history is available
            return
        }

        val listModel = sessions.map {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(it.createdAt))
            "${it.name} - $date"
        }

        val jbList = JBList(listModel)
        jbList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        JBPopupFactory.getInstance()
            .createListPopupBuilder(jbList)
            .setTitle(AutoDevBundle.message("popup.title.session.history"))
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .setItemChoosenCallback {
                val selectedIndex = jbList.selectedIndex
                if (selectedIndex != -1) {
                    val selectedSession = sessions[selectedIndex]
                    loadSessionIntoSketch(project, selectedSession)
                }
            }
            .createPopup()
            .showInBestPositionFor(e.dataContext)
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