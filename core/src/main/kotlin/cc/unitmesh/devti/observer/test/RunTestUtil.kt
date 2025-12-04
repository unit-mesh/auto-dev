package cc.unitmesh.devti.observer.test

import cc.unitmesh.devti.sketch.ui.patch.readText
import cc.unitmesh.devti.util.isInProject
import cc.unitmesh.devti.util.relativePath
import com.intellij.build.BuildView
import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import java.lang.reflect.Field

object RunTestUtil {
    fun buildFailurePrompt(project: Project, test: SMTestProxy, searchScope: GlobalSearchScope): String {
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
        val errorMsg = if (test.errorMessage.isNullOrBlank()) {
            ""
        } else {
            """## ErrorMessage:
               |```
               |${test.errorMessage}
               |```""".trimMargin()
        }
        val prompt = """Help me fix follow test issue:
                                   |$errorMsg
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
        return prompt
    }

    /**
     *```kotlin
     * (content.component.components.firstOrNull() as? NonOpaquePanel)?.components?.firstOrNull { it is JBRunnerTabs }
     *```
     */
    fun collectConsoleRelatedCode(project: Project): List<String>? {
        val content = RunContentManager.getInstance(project).selectedContent ?: return null
        val executionConsole = content.executionConsole ?: return null
        val consoleViewImpl: ConsoleViewImpl = getConsoleView(executionConsole) ?: return null
        return collectConsoleRelatedContent(project, consoleViewImpl)
    }

    fun collectConsoleRelatedContent(project: Project, consoleViewImpl: ConsoleViewImpl): List<String>? {
        val editor = consoleViewImpl.editor ?: return null

        val textRange = TextRange(0, editor.document.textLength)
        val highlighters = editor.markupModel.allHighlighters

        val fileLineMap = mutableMapOf<VirtualFile, MutableSet<Int>>()
        highlighters.forEach { highlighter ->
            if (!textRange.contains(highlighter.range!!)) return@forEach

            val hyperlinkInfo =
                EditorHyperlinkSupport.getHyperlinkInfo(highlighter) as? FileHyperlinkInfo ?: return@forEach
            val descriptor = runReadAction { hyperlinkInfo.descriptor } ?: return@forEach
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

    fun getConsoleView(executionConsole: ExecutionConsole): ConsoleViewImpl? {
        when (executionConsole) {
            is ConsoleViewImpl -> {
                return executionConsole
            }

            is BuildView -> {
                // Use reflection to access internal consoleView to avoid using Internal API
                try {
                    val consoleViewField = BuildView::class.java.getDeclaredField("consoleView")
                    consoleViewField.isAccessible = true
                    val consoleView = consoleViewField.get(executionConsole)
                    when (consoleView) {
                        is SMTRunnerConsoleView -> {
                            return getTestView(consoleView)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback: return null if reflection fails
                    return null
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
}


val RangeMarker.range: TextRange?
    get() {
        if (!isValid) return null
        val start = startOffset
        val end = endOffset
        return if (start in 0..end) {
            TextRange(start, end)
        } else {
            // Probably a race condition had happened and range marker is invalidated
            null
        }
    }