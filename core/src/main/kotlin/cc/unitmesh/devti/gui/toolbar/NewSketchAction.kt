package cc.unitmesh.devti.gui.toolbar

import cc.unitmesh.devti.history.ChatHistoryService
import cc.unitmesh.devti.history.ChatSessionHistory
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.gui.AutoDevToolWindowFactory.AutoDevToolUtil
import cc.unitmesh.devti.settings.locale.LanguageChangedCallback.componentStateChanged
import cc.unitmesh.devti.sketch.SketchToolWindow
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JButton
import javax.swing.JComponent

class NewSketchAction : AnAction(AllIcons.General.Add), CustomComponentAction {
    private val logger = logger<NewSketchAction>()

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabled = false
            return
        }

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID)
        e.presentation.isEnabled = toolWindow?.isVisible ?: false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID)
        val contentManager = toolWindow?.contentManager
        val sketchPanel =
            contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull()

        if (sketchPanel?.isDisplayingHistoryMessages != true) {
            saveCurrentSession(project, sketchPanel)
        }

        sketchPanel?.resetSketchSession()
    }

    private fun saveCurrentSession(project: Project, sketchToolWindow: SketchToolWindow?) {
        if (sketchToolWindow == null) return
        if (sketchToolWindow.isDisplayingHistoryMessages) return

        val agentStateService = project.getService(AgentStateService::class.java) ?: return
        val chatHistoryService = project.getService(ChatHistoryService::class.java) ?: return

        val messages = agentStateService.getAllMessages()
        if (messages.isNotEmpty()) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val firstUser = messages.firstOrNull { it.role.lowercase() == "user" }
            val limit = (firstUser?.content ?: timestamp).toString().take(32)
            val sessionName = "Session - $limit"
            chatHistoryService.saveSession(sessionName, messages)
            logger.info("Saved session: $sessionName")
        }
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val button: JButton = object : JButton(AllIcons.General.Add) {
            init {
                addActionListener {
                    val project = ProjectManager.getInstance().openProjects.firstOrNull()
                    if (project == null) {
                        logger.warn("Cannot get project from component: $this")
                        return@addActionListener
                    }

                    val toolWindow =
                        ToolWindowManager.getInstance(project).getToolWindow(AutoDevToolUtil.ID)
                    val contentManager = toolWindow?.contentManager
                    val sketchPanel =
                        contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull()

                    if (sketchPanel == null) {
                        return@addActionListener
                    }
                    // 只有在不是显示历史消息时才保存当前会话
                    if (!sketchPanel.isDisplayingHistoryMessages) {
                        saveCurrentSession(project, sketchPanel)
                    }
                    sketchPanel.resetSketchSession()
                }
            }

            override fun getInsets(): JBInsets {
                return JBInsets.create(2, 2)
            }
        }
        button.isFocusable = false
        button.isOpaque = false
        button.toolTipText = presentation.text
        componentStateChanged(presentation.text, button) { b, d -> b.text = d }

        val wrapper = Wrapper(button)
        wrapper.border = JBUI.Borders.empty(0, 2)
        return wrapper
    }
}