package cc.unitmesh.devti.observer

import cc.unitmesh.devti.gui.AutoDevToolWindowFactory
import cc.unitmesh.devti.gui.chat.message.ChatActionType
import cc.unitmesh.devti.gui.sendToChatWindow
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.util.relativePath
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener.TEST_STATUS
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.search.ProjectScope.getProjectScope
import com.intellij.util.messages.MessageBusConnection

class TestAgentObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null
    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        val searchScope = getProjectScope(project)
        connection?.subscribe(TEST_STATUS, object : SMTRunnerEventsAdapter() {
            override fun onSuiteFinished(suite: SMTestProxy, nodeId: String?) {
                logger<AutoDevToolWindowFactory>().info(suite.toString())
            }

            override fun onTestFailed(test: SMTestProxy) {
                val sourceCode = test.getLocation(project, searchScope)
                runInEdt {
                    sendToChatWindow(project, ChatActionType.CHAT) { contentPanel, _ ->
                        val psiElement = sourceCode?.psiElement
                        val language = psiElement?.language?.displayName ?: ""
                        val filepath = psiElement?.containingFile?.virtualFile?.relativePath(project) ?: ""
                        val code = runReadAction<@NlsSafe String> { psiElement?.text ?: "" }
                        contentPanel.setInput(
                            """Help me fix follow test issue:
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
                        )
                    }
                }
            }

            override fun onTestFinished(test: SMTestProxy, nodeId: String?) {
                logger<TestAgentObserver>().info(nodeId)
            }
        })
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
