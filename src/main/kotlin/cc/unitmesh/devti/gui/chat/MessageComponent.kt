package cc.unitmesh.devti.gui.chat

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import org.commonmark.Extension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import javax.swing.JEditorPane

fun parseMarkdown(markdown: String): String {
//    val extensions: List<Extension> = listOf(TablesExtension.create())
    val parser = Parser.builder()
//        .extensions(extensions)
        .build()

    val document: Node = parser.parse(markdown)
    val htmlRenderer = HtmlRenderer.builder().build()
    return htmlRenderer.render(document)
}

class MessageComponent(question: String, isPrompt: Boolean = false) : JEditorPane() {
    init {
        this.contentType = "text/html;charset=UTF-8"
        this.putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        this.font = UIUtil.getMenuFont()
        this.isEditable = false
        this.background = when {
            isPrompt -> JBColor(0xEAEEF7, 0x45494A)
            else -> {
                JBColor(0xE0EEF7, 0x2d2f30)
            }
        }

        this.border = JBEmptyBorder(10)
        this.text = if (isPrompt) question else parseMarkdown(question)
    }
}