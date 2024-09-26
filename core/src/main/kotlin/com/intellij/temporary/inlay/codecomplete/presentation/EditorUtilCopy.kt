package com.intellij.temporary.inlay.codecomplete.presentation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.project.Project
import kotlin.math.min

object EditorUtilCopy {
    fun indentLine(project: Project?, editor: Editor, lineNumber: Int, indent: Int, caretOffset: Int): Int {
        return indentLine(
            project,
            editor,
            lineNumber,
            indent,
            caretOffset,
            EditorActionUtil.shouldUseSmartTabs(project, editor)
        )
    }

    fun indentLine(
        project: Project?,
        editor: Editor,
        lineNumber: Int,
        indent: Int,
        caretOffset: Int,
        shouldUseSmartTabs: Boolean
    ): Int {
        val editorSettings = editor.settings
        val tabSize = editorSettings.getTabSize(project)
        val document = editor.document
        val text = document.immutableCharSequence
        var spacesEnd = 0
        var lineStart = 0
        var lineEnd = 0
        var tabsEnd = 0
        if (lineNumber < document.lineCount) {
            lineStart = document.getLineStartOffset(lineNumber)
            lineEnd = document.getLineEndOffset(lineNumber)
            spacesEnd = lineStart
            var inTabs = true
            while (spacesEnd <= lineEnd && spacesEnd != lineEnd) {
                val c = text[spacesEnd]
                if (c != '\t') {
                    if (inTabs) {
                        inTabs = false
                        tabsEnd = spacesEnd
                    }
                    if (c != ' ') {
                        break
                    }
                }
                spacesEnd++
            }
            if (inTabs) {
                tabsEnd = lineEnd
            }
        }
        var newCaretOffset = caretOffset
        if (newCaretOffset in lineStart until lineEnd && spacesEnd == lineEnd) {
            spacesEnd = newCaretOffset
            tabsEnd = min(spacesEnd, tabsEnd)
        }
        val oldLength = getSpaceWidthInColumns(text, lineStart, spacesEnd, tabSize)
        val tabsEnd2 = getSpaceWidthInColumns(text, lineStart, tabsEnd, tabSize)
        var newLength = oldLength + indent
        if (newLength < 0) {
            newLength = 0
        }
        var tabsEnd3 = tabsEnd2 + indent
        if (tabsEnd3 < 0) {
            tabsEnd3 = 0
        }
        if (!shouldUseSmartTabs) {
            tabsEnd3 = newLength
        }
        val buf = StringBuilder(newLength)
        var i = 0
        while (i < newLength) {
            if (tabSize > 0 && editorSettings.isUseTabCharacter(project) && i + tabSize <= tabsEnd3) {
                buf.append('\t')
                i += tabSize
            } else {
                buf.append(' ')
                i++
            }
        }
        val newSpacesEnd = lineStart + buf.length
        if (newCaretOffset >= spacesEnd) {
            newCaretOffset += buf.length - (spacesEnd - lineStart)
        } else if (newCaretOffset >= lineStart && newCaretOffset > newSpacesEnd) {
            newCaretOffset = newSpacesEnd
        }
        return newCaretOffset
    }

    private fun getSpaceWidthInColumns(seq: CharSequence, startOffset: Int, endOffset: Int, tabSize: Int): Int {
        var result = 0
        for (i in startOffset until endOffset) {
            if (seq[i] == '\t') {
                result = (result / tabSize + 1) * tabSize
            } else {
                result++
            }
        }
        return result
    }
}