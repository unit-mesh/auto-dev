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
import java.awt.Graphics
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JComponent

class EditorPadding(private val editor: Editor, pad: Int) :
    Box.Filler(Dimension(pad, 0), Dimension(pad, 0), Dimension(pad, 32767)) {
    init {
        setOpaque(true)
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                this@EditorPadding.repaint()
            }
        })
    }

    override fun getBackground(): Color {
        return editor.contentComponent.getBackground()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
    }
}


class EditorFragment(private val editor: EditorEx, message: CompletableMessage) {
    private val editorLineThreshold = 6
    private val expandCollapseTextLabel: JBLabel = JBLabel(message.getRole().roleName(), 0).apply {
        setOpaque(true)
        isVisible = false
    }

    private val content: BorderLayoutPanel
    private var collapsed = false

    init {
        content = object : BorderLayoutPanel() {
            override fun getPreferredSize(): Dimension {
                val preferredSize = super.getPreferredSize()
                val lineCount = editor.document.lineCount
                val shouldCollapse = lineCount > editorLineThreshold
                if (shouldCollapse && getCollapsed()) {
                    val lineHeight = editor.lineHeight
                    val insets = editor.scrollPane.getInsets()
                    val height = lineHeight * editorLineThreshold + insets.height

                    var editorMaxHeight = height + expandCollapseTextLabel.preferredSize.height + getInsets().height
                    val header = editor.headerComponent
                    if (header != null) {
                        editorMaxHeight += header.getPreferredSize().height
                    }

                    return Dimension(preferredSize.width, editorMaxHeight)
                }

                return preferredSize
            }
        }

        content.setBorder(
            JBUI.Borders.compound(
                JBUI.Borders.empty(10, 0),
                JBUI.Borders.customLine(JBColor(0xD4E1570, 0x474071))
            )
        )
        content.setOpaque(false)
        content.addToLeft((EditorPadding(editor, 5)))
        content.addToCenter((editor.component))
        content.addToRight((EditorPadding(editor, 5)))
        content.addToBottom((expandCollapseTextLabel))

        expandCollapseTextLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                setCollapsed(!getCollapsed())
            }
        })
    }

    fun getContent(): JComponent = content

    fun getCollapsed(): Boolean = collapsed

    fun setCollapsed(value: Boolean) {
        collapsed = value
        updateExpandCollapseLabel()
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
