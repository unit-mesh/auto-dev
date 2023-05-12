package cc.unitmesh.devti.editor

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.geom.Rectangle2D

class AutoDevEditorListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project = editor.project ?: return
        if (project.isDisposed) return

        val disposable = Disposer.newDisposable("devtiEditorListener")
        EditorUtil.disposeWithEditor(editor, disposable)
        editor.document.addDocumentListener((DevtiDocumentListener(editor) as DocumentListener), disposable)
    }

    class DevtiDocumentListener(val editor: Editor) : BulkAwareDocumentListener {
        override fun documentChangedNonBulk(event: DocumentEvent) {
            val project = editor.project
            if (project == null || project.isDisposed) return

//            val commandProcessor = CommandProcessor.getInstance()
//            if (commandProcessor.isUndoTransparentActionInProgress) return
//            if (commandProcessor.currentCommandName != null) return

            val changeOffset = event.offset + event.newLength
            if (editor.caretModel.offset != changeOffset) return

            LOG.warn("documentChangedNonBulk: ${event.document.text}")
            // changeOffset
            LOG.warn("changeOffset: $changeOffset")

            val render = DevtiDefaultInlayRenderer(lines = listOf("Hello, world!"))
//            editor.inlayModel.addInlineElement(changeOffset, true, 9999, render)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(AutoDevEditorListener::class.java)
    }
}

class DevtiDefaultInlayRenderer(val lines: List<String>) : EditorCustomElementRenderer {
    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0
    private val content: String = lines.joinToString("\n")
    private val KEY_CACHED_FONTMETRICS = Key.create<Map<Font, FontMetrics>>("devti.fontMetrics")

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return inlay.editor.lineHeight * lines.size.also { cachedHeight = it }
    }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val width = calculateWidth(inlay.editor, content, lines)
        return Math.max(1, width).also { cachedWidth = it }
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, region: Rectangle, surroundingTextAttributes: TextAttributes) {
        val editor = inlay.editor
        if (editor.isDisposed) return

        val attributes =
            editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND)

        renderCodeBlock(editor, content, lines, g, region, attributes)
    }

    private fun renderCodeBlock(
        editor: Editor,
        content: String,
        contentLines: List<String>,
        g: Graphics,
        region: Rectangle2D,
        attributes: TextAttributes
    ) {
        if (content.isEmpty() || contentLines.isEmpty()) {
            return
        }
        val clipBounds = g.clipBounds
        val g2 = g.create() as Graphics2D
        GraphicsUtil.setupAAPainting(g2)
        val font = getFont(editor, content)
        g2.font = font
        val metrics = fontMetrics(editor, font)
        val lineHeight = editor.lineHeight.toDouble()
        val fontBaseline = Math.ceil(font.createGlyphVector(metrics.fontRenderContext, "Alb").visualBounds.height)
        val linePadding = (lineHeight - fontBaseline) / 2.0
        val offsetX = region.x
        val offsetY = region.y + fontBaseline + linePadding
        var lineOffset = 0
        g2.clip = if (clipBounds != null && clipBounds != region) region.createIntersection(clipBounds) else region
        for (line in contentLines) {
            renderBackground(g2, attributes, offsetX, region.y + lineOffset, region.width, lineHeight)
            g2.color = attributes.foregroundColor
            g2.drawString(line, offsetX.toFloat(), (offsetY + lineOffset).toFloat())
            lineOffset = (lineOffset + lineHeight).toInt()
        }
        g2.dispose()
    }

    private fun renderBackground(
        g: Graphics2D,
        attributes: TextAttributes,
        x: Double,
        y: Double,
        width: Double,
        height: Double
    ) {
        val color = attributes.backgroundColor
        if (color != null) {
            g.color = color
            g.fillRoundRect(x.toInt(), y.toInt(), width.toInt(), height.toInt(), 1, 1)
        }
    }

    private fun calculateWidth(editor: Editor, text: String, textLines: List<String>): Int {
        val metrics = fontMetrics(editor, getFont(editor, text))
        var maxWidth = 0
        for (line in textLines) {
            maxWidth = Math.max(maxWidth, metrics.stringWidth(line))
        }

        return maxWidth
    }

    private fun getFont(editor: Editor, text: String): Font {
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN).deriveFont(2)
        val fallbackFont = UIUtil.getFontWithFallbackIfNeeded(font, text)
        val scheme = editor.colorsScheme
        return fallbackFont.deriveFont(scheme.editorFontSize.toFloat())
    }

    private fun fontMetrics(editor: Editor, font: Font): FontMetrics {
        val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
        val context = FontRenderContext(
            editorContext.transform,
            AntialiasingType.getKeyForCurrentScope(false),
            editorContext.fractionalMetricsHint
        )

        val cachedMap = KEY_CACHED_FONTMETRICS[editor, emptyMap()]
        var fontMetrics = cachedMap[font]
        if (fontMetrics == null || !context.equals(fontMetrics.fontRenderContext)) {
            fontMetrics = FontInfo.getFontMetrics(font, context)
            KEY_CACHED_FONTMETRICS.set(editor, cachedMap + mapOf(font to fontMetrics))
        }

        return fontMetrics
    }
}
