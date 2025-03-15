package cc.unitmesh.devti.sketch.ui

import cc.unitmesh.devti.fullWidth
import cc.unitmesh.devti.sketch.ui.code.MarkdownViewer
import cc.unitmesh.devti.util.parser.convertMarkdownToHtml
import com.intellij.ide.BrowserUtil
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class MarkdownPreviewHighlightSketch(val project: Project, val text: String) : ExtensionLangSketch {
    override fun getExtensionName(): String = "Markdown Highlight"
    override fun getViewText(): String = context

    private var context = text

    val webviewPanel = MarkdownViewer.createBaseComponent()

    private val editorPane = webviewPanel.apply {
        addHyperlinkListener {
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(it.url)
            }
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e == null) return
                parent?.dispatchEvent(e)
            }
        })
    }

    val previewPanel = panel {
        row {
            cell(editorPane).fullWidth()
        }.resizableRow()
    }.apply {
        border = javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)
    }

    override fun updateViewText(text: String, complete: Boolean) {
        editorPane.text = convertMarkdownToHtml(text)
        editorPane.invalidate()
        editorPane.repaint()
        this.context = text
    }

    override fun getComponent(): JComponent {
        return previewPanel
    }

    override fun updateLanguage(language: Language?, originLanguage: String?) {}

    override fun dispose() {
    }
}