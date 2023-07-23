package cc.unitmesh.devti.intentions.error

import cc.unitmesh.devti.llms.tokenizer.TokenizerImpl
import cc.unitmesh.devti.prompting.model.RuntimeErrorExplanationPrompt
import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.impl.EditorHyperlinkSupport
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.refactoring.suggested.range
import kotlin.jvm.internal.Intrinsics
import kotlin.math.max

object ErrorMessageProcessor {
    fun getErrorDescription(event: AnActionEvent): ErrorDescription? {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val selectionModel = editor.selectionModel
        val text = selectionModel.selectedText ?: return null
        val selectionStartPosition = selectionModel.selectionStartPosition ?: return null

        val lineFrom = selectionStartPosition.line
        val selectionEndPosition = selectionModel.selectionEndPosition ?: return null
        val lineTo = selectionEndPosition.line
        return ErrorDescription(text, lineFrom, lineTo, editor)
    }

    private fun extractTextFromRunPanel(
        project: Project, lineFrom: Int,
        lineTo: Int?,
        consoleEditor: Editor?
    ): String? {
        var editor = consoleEditor
        if (editor == null) editor = getConsoleEditor(project)
        if (editor == null) return null

        val document = editor.document

        return document.getText(
            TextRange(
                document.getLineStartOffset(lineFrom),
                document.getLineEndOffset(lineTo ?: (document.lineCount - 1))
            )
        )
    }

    private fun getConsoleEditor(project: Project): Editor? {
        val executionConsole = getExecutionConsole(project) ?: return null
        val consoleViewImpl: ConsoleViewImpl = executionConsole as? ConsoleViewImpl ?: return null
        return consoleViewImpl.editor
    }

    fun extracted(
        project: Project, description: ErrorDescription,
    ): RuntimeErrorExplanationPrompt? {
        val consoleLineFrom = description.consoleLineFrom
        val consoleLineTo = description.consoleLineTo
        val consoleEditor = description.editor

        val extractedText =
            extractTextFromRunPanel(project, consoleLineFrom, consoleLineTo, consoleEditor)
                ?: return null

        val extractedErrorPlaces: List<ErrorPlace> =
            extractErrorPlaces(project, consoleLineFrom, consoleLineTo, consoleEditor)

        val errorPromptConstructor = ErrorPromptConstructor(8192, TokenizerImpl.INSTANCE)

        return errorPromptConstructor.makePrompt(extractedText, extractedErrorPlaces)
    }

    private fun getFileHyperlinkInfo(rangeHighlighter: RangeHighlighter): FileHyperlinkInfo? {
        val hyperlinkInfo = EditorHyperlinkSupport.getHyperlinkInfo(rangeHighlighter)
        return when (hyperlinkInfo) {
            is FileHyperlinkInfo -> hyperlinkInfo
            else -> null
        }
    }

    private fun extractErrorPlaceFromHighlighter(
        consoleText: String, highlighter: RangeHighlighter, project: Project
    ): ErrorPlace? {
        val fileHyperlinkInfo: FileHyperlinkInfo = getFileHyperlinkInfo(highlighter) ?: return null

        val descriptor = fileHyperlinkInfo.descriptor ?: return null
        val virtualFile: VirtualFile = descriptor.file

        val lineNumber = max(0.0, descriptor.line.toDouble()).toInt()

        val range = highlighter.range!!

        val hyperlinkText = consoleText.substring(range.startOffset, range.endOffset)
        val projectFileIndex: ProjectFileIndex = ProjectFileIndex.getInstance(project)
        val isProjectFile =
            projectFileIndex.isInProject(virtualFile, project) && !projectFileIndex.isInLibrary(virtualFile)

        return ErrorPlace(
            hyperlinkText,
            lineNumber,
            isProjectFile,
            virtualFile,
            project
        )
    }

    private fun extractErrorPlaces(
        project: Project, consoleLineFrom: Int, consoleLineTo: Int?, consoleEditor: Editor?
    ): List<ErrorPlace> {
        val editor = consoleEditor ?: getConsoleEditor(project) ?: return emptyList()

        val text = editor.document.text
        val highlighters: Array<RangeHighlighter> = editor.markupModel.allHighlighters
        val result: MutableList<ErrorPlace> = ArrayList()

        val startOffset = editor.document.getLineStartOffset(consoleLineFrom)
        val endOffset = editor.document.getLineEndOffset(consoleLineTo ?: (editor.document.lineCount - 1))

        val textRange = TextRange(startOffset, endOffset)

        for (highlighter in highlighters) {
            if (textRange.contains(highlighter.range!!)) {
                extractErrorPlaceFromHighlighter(text, highlighter, project)?.let { result.add(it) }
            }
        }

        return result
    }

    private fun getExecutionConsole(project: Project): ExecutionConsole? {
        val runContentManager: RunContentManager = RunContentManager.getInstance(project)
        Intrinsics.checkNotNullExpressionValue(runContentManager, "getInstance(project)")
        val selectedContent: RunContentDescriptor? = runContentManager.selectedContent

        return selectedContent?.executionConsole
    }
}