// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.error

import cc.unitmesh.devti.llms.tokenizer.TokenizerFactory
import cc.unitmesh.devti.util.isInProject
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.settings.AutoDevSettingsState
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
import kotlin.math.max

object ErrorMessageProcessor {
    fun getErrorDescription(event: AnActionEvent): ErrorDescription? {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val selectionModel = editor.selectionModel
        val text = selectionModel.selectedText ?: return null
        val position = selectionModel.selectionStartPosition ?: return null

        val endPosition = selectionModel.selectionEndPosition ?: return null
        return ErrorDescription(text, position.line, endPosition.line, editor)
    }

    private fun extractTextFromRunPanel(
        project: Project, lineFrom: Int,
        lineTo: Int?,
        consoleEditor: Editor?
    ): String? {
        val editor = consoleEditor ?: getConsoleEditor(project) ?: return null
        val document = editor.document

        var end = if (lineTo == null) {
            document.lineCount - 1
        } else {
            if (lineTo >= document.lineCount - 1) document.lineCount - 1 else lineTo
        }

        if(lineFrom >= document.lineCount) {
            end = document.lineCount - 1
        }

        val textRange = TextRange(
            document.getLineStartOffset(lineFrom),
            document.getLineEndOffset(end)
        )

        return document.getText(textRange)
    }

    private fun getConsoleEditor(project: Project): Editor? {
        val executionConsole = getExecutionConsole(project) ?: return null
        val consoleViewImpl: ConsoleViewImpl = executionConsole as? ConsoleViewImpl ?: return null
        return consoleViewImpl.editor
    }

    fun extracted(
        project: Project, description: ErrorDescription,
    ): TextTemplatePrompt? {
//    ): RuntimeErrorExplanationPrompt? {
        val consoleLineFrom = description.consoleLineFrom
        val consoleLineTo = description.consoleLineTo
        val consoleEditor = description.editor

        val extractedText =
            extractTextFromRunPanel(project, consoleLineFrom, consoleLineTo, consoleEditor) ?: return null

        val extractedErrorPlaces: List<ErrorPlace> =
            extractErrorPlaces(project, consoleLineFrom, consoleLineTo, consoleEditor)

        val errorPromptBuilder =
            ErrorPromptBuilder(AutoDevSettingsState.maxTokenLength, TokenizerFactory.createTokenizer())

        return errorPromptBuilder.buildPrompt(extractedText, extractedErrorPlaces)
    }

    private fun getFileHyperlinkInfo(rangeHighlighter: RangeHighlighter): FileHyperlinkInfo? {
        val hyperlinkInfo = EditorHyperlinkSupport.getHyperlinkInfo(rangeHighlighter)
        if (hyperlinkInfo !is FileHyperlinkInfo) return null

        return hyperlinkInfo
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
            isInProject(virtualFile, project) && !projectFileIndex.isInLibrary(virtualFile)

        return ErrorPlace(hyperlinkText, lineNumber, isProjectFile, virtualFile, project)
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
        val selectedContent: RunContentDescriptor? = RunContentManager.getInstance(project).selectedContent
        return selectedContent?.executionConsole
    }
}