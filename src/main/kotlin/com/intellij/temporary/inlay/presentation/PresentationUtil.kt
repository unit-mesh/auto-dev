package com.intellij.temporary.inlay.presentation

import com.intellij.codeInsight.codeVision.ui.renderers.painters.CodeVisionThemeInfoProvider
import com.intellij.codeWithMe.ClientId
import com.intellij.ide.ui.AntialiasingType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.impl.FontInfo
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.font.FontRenderContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.stream.Collectors

object PresentationUtil {
    private val KEY_CACHED_FONTMETRICS = Key.create<Map<Font, FontMetrics>>("autodev.editorFontMetrics")

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

    fun replaceLeadingTabs(lines: List<String?>, tabWidth: Int): List<String> {
        return lines.stream().map { line: String? ->
            val tabCount = StringUtil.countChars(line!!, '\t', 0, true)
            if (tabCount > 0) {
                val tabSpaces = StringUtil.repeatSymbol(' ', tabCount * tabWidth)
                return@map tabSpaces + tabSpaces
            }
            line
        }.collect(Collectors.toList()) as List<String>
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


    private val getEditorFontSize2DMethod: Method?

    init {
        var method: Method? = null
        if (ApplicationInfo.getInstance().build.baselineVersion >= 221) {
            try {
                method = EditorColorsScheme::class.java.getMethod("getEditorFontSize2D", *arrayOfNulls(0))
            } catch (_: NoSuchMethodException) {
            }
        }
        getEditorFontSize2DMethod = method
    }

    fun getFont(editor: Editor, text: String): Font {
        val font = editor.colorsScheme.getFont(EditorFontType.PLAIN).deriveFont(2)
        val fallbackFont = UIUtil.getFontWithFallbackIfNeeded(font, text)
        return fallbackFont.deriveFont(fontSize(editor))
    }

    fun fontSize(editor: Editor): Float {
        val scheme = editor.colorsScheme
        if (getEditorFontSize2DMethod != null) {
            try {
                return getEditorFontSize2DMethod.invoke(scheme, *arrayOfNulls(0)) as Float
            } catch (_: IllegalAccessException) {
            } catch (_: InvocationTargetException) {
            }
        }
        return scheme.editorFontSize.toFloat()
    }
}