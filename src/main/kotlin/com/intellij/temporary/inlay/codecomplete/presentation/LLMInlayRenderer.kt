package com.intellij.temporary.inlay.codecomplete.presentation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.temporary.inlay.presentation.LLMTextInlayPainter
import org.apache.commons.lang.StringUtils
import org.jetbrains.annotations.NonNls
import java.awt.Graphics
import java.awt.Rectangle

class LLMInlayRenderer(editor: Editor, lines: List<String?>) : EditorCustomElementRenderer {
    private val lines: List<String>
    private val content: String
    fun setCachedWidth(cachedWidth: Int) {
        this.cachedWidth = cachedWidth
    }

    private val textAttributes: TextAttributes
    private var inlay: Inlay<EditorCustomElementRenderer>? = null
    fun setCachedHeight(cachedHeight: Int) {
        this.cachedHeight = cachedHeight
    }

    fun getLines(): List<String> {
        return lines
    }

    fun getContent(): String {
        return content
    }

    private var cachedWidth = -1
    private var cachedHeight = -1

    init {
        this.lines = PresentationUtil.replaceLeadingTabs(lines, 4)
        content = StringUtils.join(lines, "\n")
        textAttributes = PresentationUtil.getTextAttributes(editor)
    }

    fun getInlay(): Inlay<EditorCustomElementRenderer>? {
        return inlay
    }

    fun setInlay(inlay: Inlay<EditorCustomElementRenderer>) {
        this.inlay = inlay
    }

    fun getContentLines(): List<String> {
        return lines
    }

    override fun getContextMenuGroupId(inlay: Inlay<*>): @NonNls String {
        return "copilot.inlayContextMenu"
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return if (cachedHeight < 0) {
            inlay.editor.lineHeight * lines.size.also { cachedHeight = it }
        } else {
            cachedHeight
        }
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        if (cachedWidth < 0) {
            val width = LLMTextInlayPainter.calculateWidth(inlay.editor, content, lines)
            return Math.max(1, width).also { cachedWidth = it }
        }
        return cachedWidth
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, region: Rectangle, surroundingTextAttributes: TextAttributes) {
        val editor = inlay.editor
        if (editor.isDisposed) {
            return
        }

        LLMTextInlayPainter.renderCodeBlock(editor, content, lines, g, region, textAttributes)
    }
}