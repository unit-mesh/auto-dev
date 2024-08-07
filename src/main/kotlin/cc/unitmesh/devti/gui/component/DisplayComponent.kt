package cc.unitmesh.devti.gui.component

import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import javax.accessibility.AccessibleContext
import javax.swing.JEditorPane
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup

class DisplayComponent(question: String) : JEditorPane() {
    init {
        this.contentType = "text/plain;charset=UTF-8"
        this.putClientProperty(HONOR_DISPLAY_PROPERTIES, true)
        this.font = UIUtil.getMenuFont()
        this.isEditable = false
        this.border = JBEmptyBorder(8)
        this.text = question
        this.isOpaque = false
        this.putClientProperty(
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY, stripHtmlAndUnescapeXmlEntities(question)
        )

        if (this.caret != null) {
            this.caretPosition = 0
        }
    }

    fun updateMessage(content: String) {
        this.text = content
    }

    private fun stripHtmlAndUnescapeXmlEntities(input: String): String {
        val text = Jsoup.parse(input).text()
        return StringEscapeUtils.unescapeXml(text)
    }
}