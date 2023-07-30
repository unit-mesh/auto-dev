package cc.unitmesh.devti.gui.chat.block

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.parser.toHtml
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.xml.util.XmlStringUtil
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent
import javax.swing.text.DefaultCaret

class TextBlockView(private val block: MessageBlock) : MessageBlockView {
    private val editorPane: JEditorPane
    private val component: Component

    init {
        editorPane = createComponent()
        component = editorPane
        val messagePartTextListener = object : MessageBlockTextListener {
            override fun onTextChanged(str: String) {
                editorPane.text = parseText(str)
                editorPane.invalidate()
            }
        }

        getBlock().addTextListener(messagePartTextListener)
        messagePartTextListener.onTextChanged(getBlock().getTextContent())
    }

    override fun getBlock(): MessageBlock = block
    override fun getComponent(): Component = component

    private fun createComponent(): JEditorPane {
        val jEditorPane = Companion.createBaseComponent()
        jEditorPane.addHyperlinkListener { it: HyperlinkEvent ->
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(it.url)
            }
        }
        jEditorPane.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e == null) return
                val parent = jEditorPane.parent
                parent?.dispatchEvent(e)
            }
        })
        return jEditorPane
    }

    fun parseText(txt: String): String {
        if (getBlock().getMessage().getRole() === ChatRole.Assistant) {
            return toHtml(txt)
        }

        return XmlStringUtil.escapeString(txt)
    }

    companion object {
        fun createBaseComponent(): JEditorPane {
            val jEditorPane = JEditorPane()
            jEditorPane.setContentType("text/html")
            val build = HTMLEditorKitBuilder().build()
            build.getStyleSheet().addRule("p {margin-top: 1px}")
            jEditorPane.also {
                it.editorKit = build
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
                val caret = jEditorPane.caret
                val defaultCaret = if (caret is DefaultCaret) caret else null
                if (defaultCaret != null) {
                    defaultCaret.updatePolicy = 1
                }
            }
            return jEditorPane
        }
    }
}
