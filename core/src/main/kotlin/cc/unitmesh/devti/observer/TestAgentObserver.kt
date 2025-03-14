package cc.unitmesh.devti.observer

import cc.unitmesh.devti.observer.test.RunTestUtil
import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.util.isInProject
import cc.unitmesh.devti.util.relativePath
import com.intellij.build.BuildView
import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.refactoring.suggested.range
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Field

class TestAgentObserver : AgentObserver, Disposable {
    private var connection: MessageBusConnection? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onRegister(project: Project) {
        connection = project.messageBus.connect()
        connection?.subscribe(SMTRunnerEventsListener.TEST_STATUS, object : SMTRunnerEventsAdapter() {
            override fun onTestFailed(test: SMTestProxy) {
                GlobalScope.launch {
                    delay(3000)
                    sendResult(test, project, ProjectScope.getProjectScope(project))
                }
            }
        })
    }

    fun sendResult(test: SMTestProxy, project: Project, searchScope: GlobalSearchScope) {
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                val prompt = RunTestUtil.buildFailurePrompt(project, test, searchScope)
                sendErrorNotification(project, prompt)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
