// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.ex.EditorEx
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


class EditorFragment(private val editor: EditorEx, message: CompletableMessage) {
    private val editorLineThreshold = 6
    private val expandCollapseTextLabel: JBLabel = JBLabel(message.getRole().roleName(), 0).apply {
        isOpaque = true
        isVisible = false
    }

    private val content: BorderLayoutPanel = createContentPanel()
    private var collapsed = false

    private fun createContentPanel(): BorderLayoutPanel {
        return object : BorderLayoutPanel() {
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                if (editor.document.lineCount > editorLineThreshold && collapsed) {
                    val lineHeight = editor.lineHeight
                    val insets = editor.scrollPane.insets
                    val editorMaxHeight = calculateMaxHeight(lineHeight, insets)
                    return Dimension(preferredSize.width, editorMaxHeight)
                }
                return preferredSize
            }

            private fun calculateMaxHeight(lineHeight: Int, insets: Insets): Int {
                val height = lineHeight * editorLineThreshold + insets.height
                val headerHeight = editor.headerComponent?.preferredSize?.height ?: 0
                val labelHeight = expandCollapseTextLabel.preferredSize.height
                return height + headerHeight + labelHeight + insets().height
            }
        }.apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.empty(10, 0),
                JBUI.Borders.customLine(JBColor.border())
            )
            isOpaque = false

            addToLeft(EditorPadding(editor, 2))
            addToCenter(editor.component)
            addToRight(EditorPadding(editor, 2))
            addToBottom(expandCollapseTextLabel)
        }
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
}

val Insets.width: Int get() = left + right
val Insets.height: Int get() = top + bottom
