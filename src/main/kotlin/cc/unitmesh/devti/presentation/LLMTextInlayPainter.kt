package cc.unitmesh.devti.presentation

import cc.unitmesh.devti.presentation.PresentationUtil.getThemeInfoProvider
import com.intellij.codeInsight.codeVision.ui.model.RangeCodeVisionModel
import com.intellij.codeInsight.codeVision.ui.renderers.painters.ICodeVisionEntryBasePainter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.paint.EffectPainter2D
import java.awt.*
import javax.swing.Icon
import javax.swing.text.StyleConstants

class LLMTextInlayPainter : ICodeVisionEntryBasePainter<String> {
    override fun paint(
        editor: Editor,
        textAttributes: TextAttributes,
        g: Graphics,
        value: String,
        point: Point,
        state: RangeCodeVisionModel.InlayState,
        hovered: Boolean
    ) {
        val themeInfoProvider = getThemeInfoProvider()
        val attributes = editor.selectionModel.textAttributes

        val inSelectedBlock = textAttributes.backgroundColor == attributes.backgroundColor

        val foregroundColor = textAttributes.foregroundColor ?: if (inSelectedBlock) {
            attributes.foregroundColor ?: editor.colorsScheme.defaultForeground
        } else {
            themeInfoProvider.foregroundColor(editor, hovered)
        }

        g.color = foregroundColor
        g.font = themeInfoProvider.font(editor, 0)
        g.drawString(value, point.x, point.y)

        val textSize = size(editor, state, value)
        val g2d = g as Graphics2D
        val effectType = textAttributes.effectType

        if (effectType == null || effectType.ordinal == StyleConstants.CharacterConstants.Underline) {
            EffectPainter2D.LINE_UNDERSCORE.paint(
                g2d, point.x.toDouble(), point.y.toDouble(),
                textSize.width.toDouble(), 5.0, g2d.font
            )
        }
    }

    override fun size(editor: Editor, state: RangeCodeVisionModel.InlayState, value: String): Dimension {
        val themeInfoProvider = getThemeInfoProvider()
        val fontMetrics = editor.component.getFontMetrics(themeInfoProvider.font(editor, 0))
        return Dimension(fontMetrics.stringWidth(value), fontMetrics.height)
    }

    override fun toIcon(editor: Editor,
                        textAttributes: TextAttributes,
                        value: String,
                        state: RangeCodeVisionModel.InlayState,
                        hovered: Boolean) = object : Icon {
        var size = size(editor, state, value)

        override fun getIconHeight(): Int = size.height

        override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
            paint(editor, textAttributes, g, value, Point(x, y + (editor as EditorImpl).ascent), state, hovered)
        }

        override fun getIconWidth(): Int = size.width
    }

}

