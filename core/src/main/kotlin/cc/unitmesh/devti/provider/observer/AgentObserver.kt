package cc.unitmesh.devti.provider.observer

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface AgentObserver {
    fun onRegister(project: Project)

    fun sendErrorNotification(project: Project, prompt: String) {
        sendToChatWindow(project, ChatActionType.CHAT) { contentPanel, _ ->
            contentPanel.setInput(prompt)
        }
    }

    companion object {
        private val EP_NAME: ExtensionPointName<AgentObserver> =
            ExtensionPointName("cc.unitmesh.agentObserver")

        fun register(project: Project) {
            EP_NAME.extensions.forEach { observer ->
                observer.onRegister(project)
            }
        }
    }
}