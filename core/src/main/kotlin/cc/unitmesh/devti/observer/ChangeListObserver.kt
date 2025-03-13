package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListListener
import com.intellij.util.messages.MessageBusConnection

class ChangeListObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null

    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        connection?.subscribe(ChangeListListener.TOPIC, object : ChangeListListener {

        })
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
