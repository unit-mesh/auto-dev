package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListAdapter
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.util.messages.MessageBusConnection

class ChangeListObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null

    override fun onRegister(project: Project) {
        ChangeListManager.getInstance(project).addChangeListListener(object : ChangeListAdapter() {
            override fun defaultListChanged(
                oldDefaultList: ChangeList?,
                newDefaultList: ChangeList?,
                automatic: Boolean
            ) {
                super.defaultListChanged(oldDefaultList, newDefaultList, automatic)
            }
        }, this)
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
