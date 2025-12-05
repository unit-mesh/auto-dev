package cc.unitmesh.devti.provider.observer

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.settings.coder.coderSetting
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.logger

interface AgentObserver {
    fun onRegister(project: Project)

    fun sendErrorNotification(project: Project, prompt: String) {
        if (prompt.isBlank()) return
        if (project.coderSetting.state.enableObserver == false) return

        runInEdt {
            // or sendToChatWindow ?
            AutoDevToolWindowFactory.sendToSketchToolWindow(project, ChatActionType.CHAT) { ui, _ ->
                ui.setInput(prompt)
            }
        }
    }

    companion object {
        private val EP_NAME: ExtensionPointName<AgentObserver> =
            ExtensionPointName("cc.unitmesh.agentObserver")

        fun register(project: Project) {
            EP_NAME.extensions.forEach { observer ->
                try {
                    observer.onRegister(project)
                } catch (e: Exception) {
                    logger<AgentObserver>().warn("Failed to register AgentObserver: ${observer.javaClass.name}", e)
                }
            }
        }
    }
}