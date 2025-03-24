package cc.unitmesh.devti.sketch.ui.code

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JEditorPane
import javax.swing.text.DefaultCaret

object MarkdownWebViewer {
    fun createBaseComponent(): JEditorPane {
        val jEditorPane = JEditorPane()
        jEditorPane.setContentType("text/html")
        val htmlEditorKit = HTMLEditorKitBuilder().build()

        val backgroundColor = JBUI.CurrentTheme.ToolWindow.background()
        val bg = ColorUtil.toHex(backgroundColor)

        val editorFontName = EditorColorsManager.getInstance().schemeForCurrentUITheme.editorFontName
        val editorFontSize = EditorColorsManager.getInstance().schemeForCurrentUITheme.editorFontSize
        val fontFamilyAndSize = "font-family:'" + editorFontName + "'; font-size:" + editorFontSize + "pt;"

        val cssRules = """
            body { $fontFamilyAndSize margin: 2px 0px; line-height: 1.5; }
            
            /* Headings */
            h1 { font-size: 1.6em; font-weight: bold; }
            h2 { font-size: 1.4em; font-weight: bold; }
            h3 { font-size: 1.2em; font-weight: bold; }
            h4, h5, h6 { font-size: 1.1em; font-weight: bold; }
            
            pre { background-color: #$bg; border-radius: 4px; padding: 8px; overflow-x: auto; margin: 1em 0; }
            code { font-family: 'JetBrains Mono', Consolas, monospace; background-color: #$bg; padding: 2px 4px; border-radius: 3px; font-size: 0.9em; }
            
            /* Lists */
            ul, ol { margin-top: 0.5em; margin-bottom: 0.5em; padding-left: 2em; }
            li { margin: 0.3em 0; }
            
            /* Blockquotes */
            blockquote { border-left: 4px solid #ddd; padding-left: 1em; margin-left: 0; margin-right: 0; color: #777; }
            
            /* Tables */
            table { border-collapse: collapse; width: 100%; margin: 1em 0; }
            th, td { border: 1px solid #ddd; padding: 6px; text-align: left; }
            th { background-color: #f2f2f2; }
            
            /* Links */
            a { color: #2196F3; text-decoration: none; }
            a:hover { text-decoration: underline; }
            
            /* Horizontal rule */
            hr { border: 0; height: 1px; background-color: #$bg; margin: 1em 0; }
            
            /* Inline elements */
            strong, b { font-weight: bold; }
            em, i { font-style: italic; }
        """.trimIndent()
        
        htmlEditorKit.styleSheet.addRule(cssRules)
        
        jEditorPane.also {
            it.editorKit = htmlEditorKit
            it.isEditable = false
            it.putClientProperty("JEditorPane.honorDisplayProperties", true)
            it.isOpaque = false
            it.border = null
            val xmlEntities = StringUtil.unescapeXmlEntities(StringUtil.stripHtml("", " "))
            it.putClientProperty("AccessibleName", "")
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
