package cc.unitmesh.devti.sketch.ui.code

import com.intellij.openapi.util.text.StringUtil
import com.intellij.temporary.gui.block.LineSpacingExtension
import com.intellij.util.ui.HTMLEditorKitBuilder
import javax.swing.JEditorPane
import javax.swing.text.DefaultCaret

object MarkdownViewer {
    fun createBaseComponent(): JEditorPane {
        val jEditorPane = JEditorPane()
        jEditorPane.setContentType("text/html")
        val htmlEditorKit = HTMLEditorKitBuilder()
            .withViewFactoryExtensions(
                LineSpacingExtension(0.2f)
            ).build()

        htmlEditorKit.getStyleSheet().addRule("p {margin-top: 1px}")

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

        return jEditorPane
    }

}