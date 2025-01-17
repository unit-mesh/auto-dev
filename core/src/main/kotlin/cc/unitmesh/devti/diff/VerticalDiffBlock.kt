/**
 * Copyright 2023 Continue Dev, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.devti.diff

import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JTextArea
import kotlin.math.min

class VerticalDiffBlock(
    private val editor: Editor,
    private val project: Project,
    var startLine: Int,
    private val onAcceptReject: (VerticalDiffBlock, Boolean) -> Unit,
    private val acceptAll: () -> Unit,
) {
    val deletedLines: MutableList<String> = mutableListOf();
    val addedLines: MutableList<String> = mutableListOf();
    private val acceptButton: JButton
    private val rejectButton: JButton
    private var deletionInlay: Disposable? = null
    private var textArea: JTextArea? = null // Used for calculation of the text area height when rendering buttons
    private var hasRenderedDiffBlock: Boolean = false
    private val editorComponentInlaysManager = EditorComponentInlaysManager.from(editor, false)
    private val greenKey = createTextAttributesKey("CONTINUE_DIFF_NEW_LINE", 0x3000FF00, editor)

    init {
        val (acceptBtn, rejectBtn) = createButtons()

        acceptButton = acceptBtn
        rejectButton = rejectBtn
    }

    fun clearEditorUI() {
        deletionInlay?.dispose()
        removeGreenHighlighters()
        removeButtons()
    }

    fun updatePosition(newLineNumber: Int) {
        startLine = newLineNumber

        val (x, y) = getButtonsXYPositions()

        rejectButton.location = Point(x, y)
        acceptButton.location = Point(x + rejectButton.preferredSize.width + 5, y)

        refreshEditor()
    }

    fun deleteLineAt(line: Int) {
        val startOffset = editor.document.getLineStartOffset(line)
        val endOffset = min(editor.document.getLineEndOffset(line) + 1, editor.document.textLength)
        val deletedText = editor.document.getText(TextRange(startOffset, endOffset))

        deletedLines.add(deletedText.trimEnd())

        // Unable to ensure that text length has not changed, so we need to get it again
        editor.document.deleteString(startOffset, min(endOffset, editor.document.textLength))
    }


    fun addNewLine(text: String, line: Int) {
        if (line == editor.document.lineCount) {
            editor.document.insertString(editor.document.textLength, "\n")
        }

        val offset = editor.document.getLineStartOffset(line)

        editor.document.insertString(offset, text + "\n")
        editor.markupModel.addLineHighlighter(greenKey, line, HighlighterLayer.LAST)

        addedLines.add(text)
    }

    fun onLastDiffLine() {
        // Handles the case where we are invoking one last time on last line of diff stream, but the block has
        // already been rendered
        if (hasRenderedDiffBlock) {
            return
        }

        if (deletedLines.size > 0) {
            renderDeletedLinesInlay()
        }

        renderButtons()

        hasRenderedDiffBlock = true
    }

    fun handleReject() {
        revertDiff()
        clearEditorUI()
    }

    private fun refreshEditor() {
        editor.contentComponent.revalidate()
        editor.contentComponent.repaint()
    }

    private fun renderDeletedLinesInlay() {
        val textArea = createDeletionTextArea(deletedLines.joinToString("\n"))
        this.textArea = textArea

        val disposable = editorComponentInlaysManager.insert(startLine, textArea, true)
        deletionInlay = disposable
    }

    private fun renderButtons() {
        val (x, y) = getButtonsXYPositions()

        rejectButton.setBounds(
            x,
            y,
            rejectButton.preferredSize.width,
            rejectButton.preferredSize.height
        )

        acceptButton.setBounds(
            x + rejectButton.width + 2,
            y,
            acceptButton.preferredSize.width,
            acceptButton.preferredSize.height
        )

        editor.contentComponent.add(acceptButton)
        editor.contentComponent.add(rejectButton)

        editor.contentComponent.setComponentZOrder(acceptButton, 0)
        editor.contentComponent.setComponentZOrder(rejectButton, 0)

        refreshEditor()
    }

    private fun createButtons(): Pair<JButton, JButton> {
        val rejectBtn =
            createButton(
                cc.unitmesh.devti.AutoDevBundle.message("sketch.patch.action.reject"),
                JBColor(0x99FF0000.toInt(), 0x99FF0000.toInt())
            ).apply {
                addActionListener {
                    handleReject()
                    onAcceptReject(this@VerticalDiffBlock, false)
                }

            }

        val acceptBtn =
            createButton(
                cc.unitmesh.devti.AutoDevBundle.message("sketch.patch.action.accept"),
                JBColor(0x8AA653, 0x8AA653)
            ).apply {
                addActionListener {
                    handleAccept()
                    onAcceptReject(this@VerticalDiffBlock, true)
                }
            }

        return Pair(acceptBtn, rejectBtn)
    }

    private fun removeButtons() {
        editor.contentComponent.remove(acceptButton)
        editor.contentComponent.remove(rejectButton)

        refreshEditor()
    }

    private fun handleAccept() {
//        clearEditorUI()
        acceptAll()
    }

    private fun revertDiff() {
        WriteCommandAction.runWriteCommandAction(project) {
            // Delete the added lines
            val lineCount = editor.document.lineCount
            val textLength = editor.document.textLength
            val startOffset = if (startLine >= lineCount) {
                textLength
            } else {
                editor.document.getLineStartOffset(startLine)
            }

            val endOffset =
                editor.document.getLineEndOffset(Math.min(lineCount - 1, startLine + addedLines.size - 1)) + 1
            editor.document.deleteString(startOffset, Math.min(endOffset, textLength))

            // Add the deleted lines back
            if (deletedLines.isNotEmpty()) {
                editor.document.insertString(startOffset, deletedLines.joinToString("\n") + "\n")
            }
        }
    }

    private fun removeGreenHighlighters() {
        val highlightersToRemove = editor.markupModel.allHighlighters.filter { highlighter ->
            val highlighterLine = editor.document.getLineNumber(highlighter.startOffset)
            highlighterLine in startLine until (startLine + addedLines.size)
        }

        highlightersToRemove.forEach { editor.markupModel.removeHighlighter(it) }
    }

    private fun createDeletionTextArea(text: String) = JTextArea(text).apply {
        isEditable = false
        background = JBColor(0x30FF0000, 0x30FF0000)
        foreground = JBColor.GRAY
        border = BorderFactory.createEmptyBorder()
        lineWrap = false
        wrapStyleWord = false
        font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
    }

    private fun getButtonsXYPositions(): Pair<Int, Int> {
        val visibleArea = editor.scrollingModel.visibleArea
        val textAreaHeight = this.textArea?.height ?: 0
        val lineStartPosition = editor.logicalPositionToXY(LogicalPosition(startLine, 0))

        val xPosition =
            visibleArea.x + visibleArea.width - acceptButton.preferredSize.width - rejectButton.preferredSize.width - 20
        val yPosition = lineStartPosition.y - textAreaHeight

        return Pair(xPosition, yPosition)
    }

    private fun createButton(text: String, backgroundColor: JBColor): JButton {
        return object : JButton(text) {
            override fun paintComponent(g: Graphics) {
                val g2 = g.create() as Graphics2D
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = backgroundColor
                g2.fillRoundRect(0, 0, width - 1, height - 1, 4, 4)
                super.paintComponent(g2)
                g2.dispose()
            }
        }.apply {
            foreground = Gray._240
            font = Font("Arial", Font.BOLD, 12)
            isContentAreaFilled = false
            isOpaque = false
            border = BorderFactory.createEmptyBorder(4, 2, 4, 2)
            preferredSize = Dimension(preferredSize.width - 30, 14)
            cursor = Cursor(Cursor.HAND_CURSOR)
        }
    }
}

fun createTextAttributesKey(name: String, color: Int, editor: Editor): TextAttributesKey {
    val attributes = TextAttributes().apply {
        backgroundColor = JBColor(color, color)
    }

    return TextAttributesKey.createTextAttributesKey(name).also {
        editor.colorsScheme.setAttributes(it, attributes)
    }
}
