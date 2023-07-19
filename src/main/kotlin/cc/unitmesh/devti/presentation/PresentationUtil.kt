package cc.unitmesh.devti.presentation

import com.intellij.codeInsight.codeVision.ui.renderers.painters.CodeVisionThemeInfoProvider
import com.intellij.codeWithMe.ClientId
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.font.FontRenderContext

object PresentationUtil {
    val KEY_CACHED_FONTMETRICS = Key.create<Map<Font, FontMetrics>>("autodev.editorFontMetrics")

    fun getTextAttributes(editor: Editor): TextAttributes {
        val scheme = editor.colorsScheme
        val themeAttributes = scheme.getAttributes(DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND)
        if (themeAttributes != null && themeAttributes.foregroundColor != null) {
            return themeAttributes
        }
        val customAttributes = themeAttributes?.clone() ?: TextAttributes()
        if (customAttributes.foregroundColor == null) {
            customAttributes.foregroundColor = JBColor.GRAY as Color
        }

        return customAttributes
    }

    fun fontMetrics(editor: Editor, font: Font): FontMetrics {
        val editorContext = FontInfo.getFontRenderContext(editor.contentComponent)
        val context = FontRenderContext(
            editorContext.transform,
            AntialiasingType.getKeyForCurrentScope(false),
            editorContext.fractionalMetricsHint,
        )
        val cachedMap = KEY_CACHED_FONTMETRICS[editor, emptyMap()]
        var fontMetrics = cachedMap[font]
        if (fontMetrics == null || !context.equals(fontMetrics.fontRenderContext)) {
            fontMetrics = FontInfo.getFontMetrics(font, context)
            KEY_CACHED_FONTMETRICS.set(editor, cachedMap + mapOf(font to fontMetrics))
        }

        return fontMetrics
    }

    fun getThemeInfoProvider(): CodeVisionThemeInfoProvider {
        val serviceClass = CodeVisionThemeInfoProvider::class.java
        val service = ApplicationManager.getApplication().getService(serviceClass)
            ?: throw RuntimeException(
                "Cannot find service ${serviceClass.name} (classloader=${serviceClass.classLoader}, " +
                        "client=${ClientId.currentOrNull})"
            )

        return service
    }
}