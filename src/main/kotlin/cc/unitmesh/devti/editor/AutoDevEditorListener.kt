package cc.unitmesh.devti.editor

import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.event.*
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.FontMetrics
import java.awt.font.FontRenderContext

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

            val commandProcessor = CommandProcessor.getInstance()
            if (commandProcessor.isUndoTransparentActionInProgress) return
            if (commandProcessor.currentCommandName != null) return

            val changeOffset = event.offset + event.newLength
            if (editor.caretModel.offset != changeOffset) return

            LOG.warn("documentChangedNonBulk: ${event.document.text}")
            // changeOffset
            LOG.warn("changeOffset: $changeOffset")

            val render = DevtiDefaultInlayRenderer(lines = listOf("Hello, world!"))
            editor.inlayModel.addInlineElement(changeOffset, true, 1, render)
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
