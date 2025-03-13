package cc.unitmesh.devti.observer

import cc.unitmesh.devti.provider.observer.AgentObserver
import cc.unitmesh.devti.util.relativePath
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.refactoring.suggested.range
import com.intellij.util.messages.MessageBusConnection
import java.lang.reflect.Field

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
        runInEdt {
            getConsoleEditor(project)
            val sourceCode = test.getLocation(project, searchScope)
            val psiElement = sourceCode?.psiElement
            val language = psiElement?.language?.displayName ?: ""
            val filepath = psiElement?.containingFile?.virtualFile?.relativePath(project) ?: ""
            val code = runReadAction<String> { psiElement?.text ?: "" }
            val prompt = """Help me fix follow test issue:
                               |## ErrorMessage:
                               |```
                               |${test.errorMessage}
                               |```
                               |## stacktrace details: 
                               |${test.stacktrace}
                               |
                               |## filepath: $filepath
                               |origin code:
                               |```$language
                               |$code
                               |```
                               |""".trimMargin()

            sendErrorNotification(project, prompt)
        }
    }

    /**
     *```kotlin
     * (content.component.components.firstOrNull() as? NonOpaquePanel)?.components?.firstOrNull { it is JBRunnerTabs }
     *```
     */
    private fun getConsoleEditor(project: Project): Editor? {
        val content = RunContentManager.getInstance(project).selectedContent ?: return null
        val executionConsole = content.executionConsole ?: return null
        val consoleViewImpl: ConsoleViewImpl = getConsoleView(executionConsole) ?: return null
        val editor = consoleViewImpl.editor ?: return null

        val startOffset = 0
        val allText = editor.document.text
        val endOffset = editor.document.textLength
        val textRange = TextRange(startOffset, endOffset)
        val highlighters: Array<RangeHighlighter> = editor.markupModel.allHighlighters

        for (highlighter in highlighters) {
            if (textRange.contains(highlighter.range!!)) {
//                val hyperlinkInfo: FileHyperlinkInfo? = EditorHyperlinkSupport.getHyperlinkInfo(highlighter) as FileHyperlinkInfo
//                val descriptor = hyperlinkInfo?.descriptor ?: return null
                val range = highlighter.range!!
                val hyperlinkText = allText.substring(range.startOffset, range.endOffset)
                println("hyperlinkText: $hyperlinkText")
            }
        }

        return consoleViewImpl.editor
    }

    private fun getConsoleView(executionConsole: ExecutionConsole): ConsoleViewImpl? {
        if (executionConsole is SMTRunnerConsoleView) {
            try {
                val resultsViewerClass = executionConsole.resultsViewer::class.java
                val myConsoleViewField: Field = resultsViewerClass.getDeclaredField("myConsoleView")
                myConsoleViewField.isAccessible = true
                val myConsoleView = myConsoleViewField.get(executionConsole.resultsViewer) as? ConsoleViewImpl
                return myConsoleView
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return executionConsole as? ConsoleViewImpl
    }

    override fun dispose() {
        connection?.disconnect()
    }
}
