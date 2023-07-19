package cc.unitmesh.devti.editor.presentation

import cc.unitmesh.devti.editor.presentation.PresentationUtil.fontMetrics
import cc.unitmesh.devti.editor.presentation.PresentationUtil.getTextAttributes
import com.intellij.codeInsight.codeVision.ui.model.RangeCodeVisionModel
import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point

class LLMTextPresentation(
    private val editor: Editor,
    private val text: String,
    private val hovered: Boolean
) : BasePresentation() {
    private val inlayState: RangeCodeVisionModel.InlayState = RangeCodeVisionModel.InlayState.NORMAL
    private val textPainter: LLMTextInlayPainter = LLMTextInlayPainter()

    override val width: Int = size().width
    override val height: Int = size().height

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        val visionThemeInfoProvider = PresentationUtil.getThemeInfoProvider()
        val updatedAttributes = getTextAttributes(editor)
        val fontMetrics = fontMetrics(editor, visionThemeInfoProvider.font(editor, 0))
        textPainter.paint(editor, updatedAttributes, g, text, Point(0, fontMetrics.height), inlayState, hovered)
    }

    override fun toString(): String = "LLMText($text)"

    private fun size(): Dimension {
        return textPainter.size(editor, inlayState, text)
    }
}
