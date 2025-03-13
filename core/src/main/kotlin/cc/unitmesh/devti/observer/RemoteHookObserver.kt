package cc.unitmesh.devti.observer

import cc.unitmesh.devti.observer.agent.AgentProcessor
import cc.unitmesh.devti.provider.observer.AgentObserver
import com.intellij.openapi.project.Project

/**
 * Remote Hook observer will receive the remote hook event and process it.
 * like:
 * - [ ] Jira issue
 * - [ ] GitHub/Gitlab issue
 * and Trigger after processor, and send the notification to the chat window.
 */
class RemoteHookObserver : AgentObserver {
    override fun onRegister(project: Project) {
//        TODO("Not yet implemented")
    }
}

class IssueWorker : AgentProcessor {
    override fun process() {
//        TODO("Not yet implemented")
    }
}