package cc.unitmesh.devti.sketch.ui.code

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import javax.swing.JEditorPane
import javax.swing.text.DefaultCaret

object MarkdownViewer {
    fun createBaseComponent(): JEditorPane {
        val jEditorPane = JEditorPane()
        jEditorPane.setContentType("text/html")
        val htmlEditorKit = HTMLEditorKitBuilder().build()

        val backgroundColor = UIUtil.getPanelBackground()

        val editorFontName = EditorColorsManager.getInstance().schemeForCurrentUITheme.editorFontName
        val editorFontSize = EditorColorsManager.getInstance().schemeForCurrentUITheme.editorFontSize

        val fontFamilyAndSize = "font-family:'" + editorFontName + "'; font-size:" + editorFontSize + "pt;"
        val backgroundColorCss = "background-color: #" + ColorUtil.toHex(backgroundColor) + ";"
        htmlEditorKit.getStyleSheet().addRule("code { $backgroundColorCss$fontFamilyAndSize}")
        htmlEditorKit.getStyleSheet().addRule("code.language-markdown { background-color: #ffffff; $fontFamilyAndSize}")
        htmlEditorKit.getStyleSheet().addRule("p { margin-top: 1px }")

        jEditorPane.also {
            it.editorKit = htmlEditorKit
            it.isEditable = false
            it.putClientProperty("JEditorPane.honorDisplayProperties", true)
            it.isOpaque = false
            it.border = null
            it.putClientProperty(
                "AccessibleName",
                StringUtil.unescapeXmlEntities(StringUtil.stripHtml("", " "))
            )
            it.text = ""
        }

        if (jEditorPane.caret != null) {
            jEditorPane.setCaretPosition(0)
            (jEditorPane.caret as? DefaultCaret)?.updatePolicy = 1
        }

        jEditorPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
        return jEditorPane
    }

}