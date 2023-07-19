package cc.unitmesh.devti.presentation

import com.intellij.codeInsight.hints.presentation.BasePresentation
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Graphics2D

class AutoDevTextPresentation : BasePresentation() {
    override val height: Int
        get() = TODO("Not yet implemented")
    override val width: Int
        get() = TODO("Not yet implemented")

    override fun paint(g: Graphics2D, attributes: TextAttributes) {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }
}

private fun getTextAttributes(editor: Editor): TextAttributes {
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
