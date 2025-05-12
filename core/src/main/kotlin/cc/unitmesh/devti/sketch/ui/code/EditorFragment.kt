package cc.unitmesh.devti.sketch.ui.code

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JComponent

class EditorPadding(private val editor: Editor, pad: Int) :
    Box.Filler(Dimension(pad, pad), Dimension(pad, pad), Dimension(pad, pad)) {
    init {
        setOpaque(true)
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                this@EditorPadding.repaint()
            }
        })
    }

    override fun getBackground(): Color = editor.contentComponent.getBackground()
}


class EditorFragment(
    var editor: EditorEx,
    private val editorLineThreshold: Int = EDITOR_LINE_THRESHOLD,
    private val previewEditor: FileEditor? = null,
) {
    private val expandCollapseTextLabel: JBLabel = JBLabel("", 0).apply {
        isOpaque = true
        isVisible = false
        border = JBUI.Borders.customLine(JBColor.border(), 0, 1, 1, 1)
    }

    private val content: BorderLayoutPanel = createContentPanel().also {
        it.border = JBUI.Borders.empty()
    }

    private var collapsed = false

    private fun createContentPanel(): BorderLayoutPanel {
        return object : BorderLayoutPanel() {
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                if (editor.document.lineCount > editorLineThreshold && collapsed) {
                    return calculatePreferredSize(preferredSize, this@EditorFragment.editorLineThreshold)
                }
                return preferredSize
            }
        }.apply {
            isOpaque = true
            if (previewEditor != null) {
                addToCenter(previewEditor.component)
            } else {
                addToCenter(editor.component)
            }
            addToBottom(expandCollapseTextLabel)
        }
    }

    private fun calculatePreferredSize(preferredSize: Dimension, lineThreshold: Int): Dimension {
        val lineHeight = editor.lineHeight
        val insets = editor.scrollPane.insets
        val editorMaxHeight = calculateMaxHeight(lineHeight, insets, lineThreshold)
        return Dimension(preferredSize.width, editorMaxHeight)
    }

    private fun calculateMaxHeight(lineHeight: Int, insets: Insets, lineThreshold: Int): Int {
        val height = lineHeight * lineThreshold + insets.height
        val headerHeight = editor.headerComponent?.preferredSize?.height ?: 0
        val labelHeight = expandCollapseTextLabel.preferredSize.height
        return height + headerHeight + labelHeight + insets.height
    }

    init {
        expandCollapseTextLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                toggleCollapsedState()
            }
        })
    }

    fun getContent(): JComponent = content

    fun isCollapsed(): Boolean = collapsed

    fun setCollapsed(value: Boolean) {
        if (collapsed != value) {
            collapsed = value
            updateExpandCollapseLabel()
        }
    }

    private fun toggleCollapsedState() {
        setCollapsed(!collapsed)
    }

    fun updateExpandCollapseLabel() {
        val linesCount = editor.document.lineCount
        expandCollapseTextLabel.isVisible = linesCount > editorLineThreshold
        expandCollapseTextLabel.text = if (collapsed) "More lines" else ""
        expandCollapseTextLabel.icon = if (collapsed) AllIcons.General.ChevronDown else AllIcons.General.ChevronUp
    }

    fun resizeForNewThreshold(newThreshold: Int) {
        content.preferredSize = calculatePreferredSize(content.preferredSize, newThreshold)

        expandCollapseTextLabel.isVisible = true
        expandCollapseTextLabel.text = "More lines"
        expandCollapseTextLabel.icon = AllIcons.General.ChevronDown

        content.revalidate()
        content.repaint()
    }

    companion object {
        private const val EDITOR_LINE_THRESHOLD = 6
    }
}

val Insets.width: Int get() = left + right
val Insets.height: Int get() = top + bottom
