package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

/**
 * When test can use an unknown dependency, it will trigger the observer to store the error.
 */
class AddDependencyAgentObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null
    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        connection?.subscribe(ProjectDataImportListener.TOPIC, object : ProjectDataImportListener {
            override fun onImportFailed(projectPath: String?, t: Throwable) {
                val prompt = """Help me fix follow dependency issue:
                               |## ErrorMessage:
                               |```
                               |${t.message}
                               |""".trimMargin()

                sendErrorNotification(project, prompt)
            }
        })
    }

    override fun dispose() {
        connection?.disconnect()
    }
}