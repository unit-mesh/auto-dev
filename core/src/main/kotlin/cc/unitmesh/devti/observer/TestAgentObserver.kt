package cc.unitmesh.devti.observer

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
import com.intellij.openapi.editor.markup.RangeHighlighter
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

    private fun sendResult(test: SMTestProxy, project: Project, searchScope: GlobalSearchScope) {
        val task = object : Task.Backgroundable(project, "Processing context", false) {
            override fun run(indicator: ProgressIndicator) {
                val relatedCode = collectConsoleRelatedCode(project) ?: emptyList()
                val sourceCode = runReadAction { test.getLocation(project, searchScope) }
                val psiElement = sourceCode?.psiElement
                val language = psiElement?.language?.displayName ?: ""
                val filepath = psiElement?.containingFile?.virtualFile?.relativePath(project) ?: ""
                val code = runReadAction<String> { psiElement?.text ?: "" }
                val formatedRelatedCode = "\n## related code:\n```$language\n${relatedCode.joinToString("\n")}\n```"
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
                               |${formatedRelatedCode}
                               |""".trimMargin()

                sendErrorNotification(project, prompt)
            }
        }

        ProgressManager.getInstance()
            .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    /**
     *```kotlin
     * (content.component.components.firstOrNull() as? NonOpaquePanel)?.components?.firstOrNull { it is JBRunnerTabs }
     *```
     */
    private fun collectConsoleRelatedCode(project: Project): List<String>? {
        val content = RunContentManager.getInstance(project).selectedContent ?: return null
        val executionConsole = content.executionConsole ?: return null
        val consoleViewImpl: ConsoleViewImpl = getConsoleView(executionConsole) ?: return null
        val editor = consoleViewImpl.editor ?: return null

        val startOffset = 0
        val endOffset = editor.document.textLength
        val textRange = TextRange(startOffset, endOffset)
        val highlighters: Array<RangeHighlighter> = editor.markupModel.allHighlighters

        /// todo: first collect file path and line number, then build by range
        val relatedCode = highlighters.mapNotNull { highlighter ->
            if (textRange.contains(highlighter.range!!)) {
                // call be .DiffHyperlink
                val hyperlinkInfo: FileHyperlinkInfo? = EditorHyperlinkSupport.getHyperlinkInfo(highlighter) as? FileHyperlinkInfo
                val descriptor = hyperlinkInfo?.descriptor ?: return@mapNotNull null
                val virtualFile: VirtualFile = descriptor.file

                val isProjectFile = runReadAction { project.isInProject(virtualFile) }
                if (isProjectFile) {
                    val lineNumber = descriptor.line
                    val allText = virtualFile.readText()
                    val startLine = if (lineNumber - 10 < 0) {
                        0
                    } else {
                        lineNumber - 10
                    }
                    //  endLine should be less than allText.lines().size
                    val endLine = if (lineNumber + 10 > allText.lines().size) {
                        allText.lines().size
                    } else {
                        lineNumber + 10
                    }

                    return@mapNotNull allText.lines().subList(startLine, endLine).joinToString("\n")
                } else {
                    return@mapNotNull null
                }
            } else {
                return@mapNotNull null
            }
        }

        return relatedCode.distinct()
    }


    private fun getConsoleView(executionConsole: ExecutionConsole): ConsoleViewImpl? {
        when (executionConsole) {
            is ConsoleViewImpl -> {
                return executionConsole
            }

            is BuildView -> {
                when (executionConsole.consoleView) {
                    is SMTRunnerConsoleView -> {
                        return getTestView(executionConsole.consoleView as SMTRunnerConsoleView)
                    }
                }
            }

            is SMTRunnerConsoleView -> {
                return getTestView(executionConsole)
            }
        }

        return null
    }

    private fun getTestView(executionConsole: SMTRunnerConsoleView): ConsoleViewImpl? {
        try {
            val resultsViewerClass = executionConsole.resultsViewer::class.java
            val myConsoleViewField: Field = resultsViewerClass.getDeclaredField("myConsoleView")
            myConsoleViewField.isAccessible = true
            val myConsoleView = myConsoleViewField.get(executionConsole.resultsViewer) as? ConsoleViewImpl
            return myConsoleView
        } catch (e: Exception) {
            return null
        }
    }

// ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.DEBUG)

    override fun dispose() {
        connection?.disconnect()
    }
}
