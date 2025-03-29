package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project

class ChangeListObserver : AgentObserver, Disposable {
    override fun onRegister(project: Project) {

    }

    override fun dispose() {
    }
}
