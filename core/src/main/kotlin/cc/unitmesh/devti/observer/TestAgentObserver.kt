package cc.unitmesh.devti.observer

import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.util.relativePath
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.messages.MessageBusConnection

class TestAgentObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null
    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        connection?.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {
            override fun onTestFailed(test: SMTestProxy) {
                sendResult(test, project, ProjectScope.getProjectScope(project))
            }
        })
    }

    private fun sendResult(test: SMTestProxy, project: Project, searchScope: GlobalSearchScope) {
        val sourceCode = test.getLocation(project, searchScope)
        runInEdt {
            val psiElement = sourceCode?.psiElement
            val language = psiElement?.language?.displayName ?: ""
            val filepath = psiElement?.containingFile?.virtualFile?.relativePath(project) ?: ""
            val code = runReadAction<String> { psiElement?.text ?: "" }
            val prompt = """Help me fix follow test issue:
                               | ErrorMessage:
                               |```
                               |${test.errorMessage}
                               |```
                               |stacktrace details: 
                               |${test.stacktrace}
                               |
                               |// filepath: $filepath
                               |origin code:
                               |```$language
                               |$code
                               |```
                               |""".trimMargin()


            sendErrorNotification(project, prompt)
        }
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
