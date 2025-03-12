package cc.unitmesh.devti.provider.observer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface AgentObserver {
    fun onRegister(project: Project)

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