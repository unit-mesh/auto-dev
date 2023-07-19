package cc.unitmesh.devti.editor.presentation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EditorActionUtil
import com.intellij.openapi.project.Project

object EditorUtilCopy {
    @JvmOverloads
    fun indentLine(
        project: Project?,
        editor: Editor,
        lineNumber: Int,
        indent: Int,
        caretOffset: Int,
        shouldUseSmartTabs: Boolean = EditorActionUtil.shouldUseSmartTabs(project, editor),
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
            while (spacesEnd <= lineEnd &&
                spacesEnd != lineEnd
            ) {
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
        if (newCaretOffset >= lineStart && newCaretOffset < lineEnd && spacesEnd == lineEnd) {
            spacesEnd = newCaretOffset
            tabsEnd = Math.min(spacesEnd, tabsEnd)
        }
        val oldLength = getSpaceWidthInColumns(text, lineStart, spacesEnd, tabSize)
        tabsEnd = getSpaceWidthInColumns(text, lineStart, tabsEnd, tabSize)
        var newLength = oldLength + indent
        if (newLength < 0) {
            newLength = 0
        }
        tabsEnd += indent
        if (tabsEnd < 0) {
            tabsEnd = 0
        }
        if (!shouldUseSmartTabs) {
            tabsEnd = newLength
        }
        val buf = StringBuilder(newLength)
        var i = 0
        while (i < newLength) {
            if (tabSize > 0 && editorSettings.isUseTabCharacter(project) && i + tabSize <= tabsEnd) {
                buf.append('\t')
                i += tabSize
                continue
            }
            buf.append(' ')
            i++
        }
        val newSpacesEnd = lineStart + buf.length
        if (newCaretOffset >= spacesEnd) {
            newCaretOffset += buf.length - spacesEnd - lineStart
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
