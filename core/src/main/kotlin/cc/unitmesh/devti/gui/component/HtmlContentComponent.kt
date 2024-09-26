package cc.unitmesh.devti.gui.component

import com.intellij.ide.BrowserUtil
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent

class HtmlContentComponent(val html: String) : JEditorPane() {
    init {
        setOpaque(false)
        isEditable = false
        setContentType("text/html")
        setEditorKit(createEditorKitForContentType("text/html"))
        putClientProperty("JEditorPane.honorDisplayProperties", true)
        setFont(UIManager.getFont("Label.font"))
        text = html

        addHyperlinkListener { obj: HyperlinkEvent ->
            if (obj.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(obj.url)
            }
        }
    }
}