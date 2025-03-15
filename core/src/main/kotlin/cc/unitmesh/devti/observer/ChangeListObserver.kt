package cc.unitmesh.devti.observer

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.observer.agent.AgentStateService
import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeList
import com.intellij.openapi.vcs.changes.ChangeListAdapter
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.util.messages.MessageBusConnection

class ChangeListObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null

    override fun onRegister(project: Project) {
        ChangeListManager.getInstance(project).addChangeListListener(object : ChangeListAdapter() {
            override fun changesAdded(changes: Collection<Change?>?, toList: ChangeList?) {
                super.changesAdded(changes, toList)
                if (toList != null) {
                    project.getService(AgentStateService::class.java).updateChanges(toList.changes)
                }
            }

            override fun changesRemoved(changes: Collection<Change?>?, fromList: ChangeList?) {
                super.changesRemoved(changes, fromList)
                if (fromList != null) {
                    project.getService(AgentStateService::class.java).updateChanges(fromList.changes)
                }
            }
        }, this)
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
