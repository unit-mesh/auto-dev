package com.intellij.temporary.inlay.presentation

import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.RangeCodeVisionModel
import com.intellij.codeInsight.codeVision.ui.renderers.painters.ICodeVisionEntryBasePainter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.temporary.inlay.codecomplete.presentation.PresentationUtil.fontMetrics
import com.intellij.temporary.inlay.codecomplete.presentation.PresentationUtil.getFont
import com.intellij.temporary.inlay.codecomplete.presentation.PresentationUtil.getThemeInfoProvider
import com.intellij.ui.paint.EffectPainter2D
import com.intellij.util.ui.GraphicsUtil
import java.awt.*
import java.awt.geom.Rectangle2D
import javax.swing.text.StyleConstants
import kotlin.math.ceil

class LLMTextInlayPainter : ICodeVisionEntryBasePainter<String> {

    override fun paint(
        editor: Editor,
        textAttributes: TextAttributes,
        g: Graphics,
        value: String,
        point: Point,
        state: RangeCodeVisionModel.InlayState,
        hovered: Boolean,
        hoveredEntry: CodeVisionEntry?
    ) {
        renderCodeBlock(
            editor,
            value,
            value.split("\n"),
            g,
            Rectangle2D.Double(point.x.toDouble(), point.y.toDouble(), 0.0, 0.0),
            textAttributes,
        )
    }

    override fun size(editor: Editor, state: RangeCodeVisionModel.InlayState, value: String): Dimension {
        val themeInfoProvider = getThemeInfoProvider()
        val fontMetrics = editor.component.getFontMetrics(themeInfoProvider.font(editor, 0))
        return Dimension(fontMetrics.stringWidth(value), fontMetrics.height)
    }

    companion object {
        fun calculateWidth(editor: Editor, text: String, textLines: List<String>): Int {
            val metrics = fontMetrics(editor, getFont(editor, text))
            var maxWidth = 0
            for (line in textLines) {
                maxWidth = Math.max(maxWidth, metrics.stringWidth(line))
            }

            return maxWidth
        }

        private fun renderEffects(
            g2d: Graphics2D,
            x: Double,
            baseline: Double,
            width: Double,
            charHeight: Int,
            descent: Int,
            textAttributes: TextAttributes,
            font: Font?,
        ) {
            val effectType = textAttributes.effectType
            if (effectType == null || effectType.ordinal == StyleConstants.CharacterConstants.Underline) {
                EffectPainter2D.LINE_UNDERSCORE.paint(
                    g2d, x, baseline,
                    width, 5.0, font
                )
            }
        }

        private fun renderBackground(
            g: Graphics2D,
            attributes: TextAttributes,
            x: Double,
            y: Double,
            width: Double,
            height: Double,
        ) {
            val color = attributes.backgroundColor
            if (color != null) {
                g.color = color
                g.fillRoundRect(x.toInt(), y.toInt(), width.toInt(), height.toInt(), 1, 1)
            }
        }

        fun renderCodeBlock(
            editor: Editor,
            content: String,
            contentLines: List<String>,
            g: Graphics,
            region: Rectangle2D,
            textAttributes: TextAttributes,
        ) {
            if (content.isEmpty() || contentLines.isEmpty()) return

            val themeInfoProvider = getThemeInfoProvider()
            val attributes = editor.selectionModel.textAttributes

            val inSelectedBlock = textAttributes.backgroundColor == attributes.backgroundColor
            val foregroundColor = textAttributes.foregroundColor ?: if (inSelectedBlock) {
                attributes.foregroundColor ?: editor.colorsScheme.defaultForeground
            } else {
                themeInfoProvider.foregroundColor(editor, false)
            }

            val clipBounds = g.clipBounds
            val g2 = g.create() as Graphics2D
            GraphicsUtil.setupAAPainting(g2)
            val font = themeInfoProvider.font(editor, 0)
            g2.font = font

            val metrics = fontMetrics(editor, font)
            val lineHeight = editor.lineHeight.toDouble()
            val fontBaseline = ceil(font.createGlyphVector(metrics.fontRenderContext, "Alb").visualBounds.height)
            val linePadding = (lineHeight - fontBaseline) / 2.0

            val offsetX = region.x
            val offsetY = region.y + fontBaseline + linePadding

            var lineOffset = 0
            g2.clip = if (clipBounds != null && clipBounds != region) region.createIntersection(clipBounds) else region

            for (line in contentLines) {
                renderBackground(g2, attributes, offsetX, region.y + lineOffset, region.width, lineHeight)
                g2.color = foregroundColor
                g2.drawString(line, offsetX.toFloat(), (offsetY + lineOffset).toFloat())
                if (editor is EditorImpl) {
                    renderEffects(
                        g2,
                        offsetX,
                        offsetY + lineOffset,
                        metrics.stringWidth(line).toDouble(),
                        editor.charHeight,
                        editor.descent,
                        attributes,
                        font,
                    )
                }
                lineOffset = (lineOffset + lineHeight).toInt()
            }
            g2.dispose()
        }
    }

}

