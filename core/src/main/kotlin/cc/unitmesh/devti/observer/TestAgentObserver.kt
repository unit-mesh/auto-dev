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
                val formatedRelatedCode = if (relatedCode.isNotEmpty()) {
                    "\n## Related Code:\n${relatedCode.joinToString("\n\n")}"
                } else {
                    ""
                }
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
                               |$formatedRelatedCode
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

        val textRange = TextRange(0, editor.document.textLength)
        val highlighters = editor.markupModel.allHighlighters

        val fileLineMap = mutableMapOf<VirtualFile, MutableSet<Int>>()
        highlighters.forEach { highlighter ->
            if (!textRange.contains(highlighter.range!!)) return@forEach
            
            val hyperlinkInfo = EditorHyperlinkSupport.getHyperlinkInfo(highlighter) as? FileHyperlinkInfo ?: return@forEach
            val descriptor = hyperlinkInfo.descriptor ?: return@forEach
            val virtualFile = descriptor.file
            
            if (runReadAction { project.isInProject(virtualFile) }) {
                fileLineMap.computeIfAbsent(virtualFile) { mutableSetOf() }.add(descriptor.line)
            }
        }

        return fileLineMap.map { (virtualFile, lineNumbers) ->
            val contentLines = virtualFile.readText().lines()
            val context = buildString {
                append("## File: ${runReadAction { virtualFile.relativePath(project) }}\n")
                val displayLines = mutableSetOf<Int>()
                lineNumbers.forEach { lineNum ->
                    val range = (maxOf(0, lineNum - 10) until minOf(contentLines.size, lineNum + 10))
                    displayLines.addAll(range)
                }
                
                displayLines.sorted().forEach { lineIdx ->
                    contentLines.getOrNull(lineIdx)?.let { line ->
                        append("${lineIdx + 1}: $line\n")
                    }
                }
            }
            
            context
        }
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

    override fun dispose() {
        connection?.disconnect()
    }
}
