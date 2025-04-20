package cc.unitmesh.devti.inlay.codecomplete.presentation

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import cc.unitmesh.devti.gui.LLMTextInlayPainter
import java.awt.Graphics
import java.awt.Rectangle

class LLMInlayRenderer(editor: Editor, lines: List<String?>) : EditorCustomElementRenderer {
    private val lines: List<String> = PresentationUtil.replaceLeadingTabs(lines, 4)
    private val content: String = lines.joinToString("\n")
    private val textAttributes: TextAttributes = PresentationUtil.getTextAttributes(editor)
    var inlay: Inlay<EditorCustomElementRenderer>? = null

    private var cachedWidth = -1
    private var cachedHeight = -1

    override fun getContextMenuGroupId(inlay: Inlay<*>): String = "autodev.inlayContextMenu"

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
            return 1.coerceAtLeast(width).also { cachedWidth = it }
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