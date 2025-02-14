// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import cc.unitmesh.devti.gui.chat.message.ChatRole
import cc.unitmesh.devti.util.parser.convertMarkdownToHtml
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.ExtendableHTMLViewFactory
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.xml.util.XmlStringUtil
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JEditorPane
import javax.swing.SizeRequirements
import javax.swing.event.HyperlinkEvent
import javax.swing.text.DefaultCaret
import javax.swing.text.Element
import javax.swing.text.View
import javax.swing.text.html.ParagraphView
import kotlin.math.max

class TextBlockView(private val block: MessageBlock) : MessageBlockView {
    private val editorPane: JEditorPane
    private val component: Component

    init {
        editorPane = createComponent()
        component = editorPane
        val messagePartTextListener = MessageBlockTextListener { str ->
            editorPane.text = parseText(str)
            editorPane.invalidate()
        }

        getBlock().addTextListener(messagePartTextListener)
        messagePartTextListener.onTextChanged(getBlock().getTextContent())
    }

    override fun getBlock(): MessageBlock = block
    override fun getComponent(): Component = component

    private fun createComponent(): JEditorPane {
        val jEditorPane = createBaseComponent()
        jEditorPane.addHyperlinkListener { it: HyperlinkEvent ->
            if (it.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(it.url)
            }
        }
        jEditorPane.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e == null) return
                jEditorPane.parent?.dispatchEvent(e)
            }
        })
        return jEditorPane
    }

    fun parseText(txt: String): String {
        if (getBlock().getMessage().getRole() === ChatRole.Assistant) {
            return convertMarkdownToHtml(txt)
        }

        return XmlStringUtil.escapeString(txt)
    }

    companion object {
        fun createBaseComponent(): JEditorPane {
            val jEditorPane = JEditorPane()
            jEditorPane.setContentType("text/html")
            val htmlEditorKit = HTMLEditorKitBuilder().withViewFactoryExtensions(
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
}


internal class LineSpacingExtension(val lineSpacing: Float) : ExtendableHTMLViewFactory.Extension {

    override operator fun invoke(elem: Element, defaultView: View): View? {
        return if (defaultView !is ParagraphView) null
        else object : ParagraphView(elem) {
            override fun calculateMinorAxisRequirements(axis: Int, requirements: SizeRequirements?): SizeRequirements {
                val sizeRequirements = requirements ?: SizeRequirements()

                sizeRequirements.minimum = layoutPool.getMinimumSpan(axis).toInt()
                sizeRequirements.preferred = max(sizeRequirements.minimum, layoutPool.getPreferredSpan(axis).toInt())
                sizeRequirements.maximum = Int.MAX_VALUE
                sizeRequirements.alignment = 0.5f

                return sizeRequirements
            }

            override fun setLineSpacing(ls: Float) {
                super.setLineSpacing(this@LineSpacingExtension.lineSpacing)
            }
        }
    }
}
