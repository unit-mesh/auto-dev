package cc.unitmesh.devti.gui.chat

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import javax.accessibility.AccessibleContext
import javax.swing.JEditorPane

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
            AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
            StringUtil.unescapeXmlEntities(StringUtil.stripHtml(question, " "))
        )

        if (this.caret != null) {
            this.caretPosition = 0
        }
    }

    fun updateMessage(content: String) {
        this.text = content
    }
}