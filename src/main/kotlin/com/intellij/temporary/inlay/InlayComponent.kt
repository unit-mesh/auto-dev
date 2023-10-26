package com.intellij.temporary.inlay

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.observable.util.whenDisposed
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.JComponent
import javax.swing.JScrollPane
import kotlin.math.max
import kotlin.math.min

class InlayComponent<T : JComponent?>(@JvmField var component: T) : JComponent(), EditorCustomElementRenderer {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int = this.maximumWidth
    override fun calcHeightInPixels(inlay: Inlay<*>): Int = this.component!!.preferredHeight
    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        if (bounds == inlay.bounds || inlay.bounds == null) return
        this.component!!.size = inlay.bounds!!.size
    }

    init {
        setOpaque(false)
    }

    fun updateWidth(maxWidth: Int) {
        this.maximumWidth = maxWidth
        this.component!!.size = Dimension(min(this.component!!.preferredWidth, maxWidth), this.component!!.height)
    }

    companion object {
        fun <T : JComponent?> add(
            editor: EditorEx,
            offset: Int,
            properties: InlayProperties,
            component: T,
        ): Inlay<InlayComponent<T>>? {
            val inlayComponent: InlayComponent<T> = InlayComponent(component)
            inlayComponent.updateWidth(calculateMaxWidth(editor.scrollPane))

            val inlayElement =
                editor.inlayModel.addBlockElement(offset, properties, inlayComponent)
                    ?: return null

            inlayComponent.add(component)
            editor.contentComponent.add(inlayComponent)

            val componentListener: ComponentListener = object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    inlayComponent.updateWidth(calculateMaxWidth(editor.scrollPane))
                    inlayElement.update()
                }
            }

            editor.scrollPane.viewport.addComponentListener(componentListener)
            inlayElement.whenDisposed {
                editor.contentComponent.remove(inlayComponent)
                editor.scrollPane.viewport.removeComponentListener(componentListener)
            }

            return inlayElement
        }

        fun calculateMaxWidth(scrollPane: JScrollPane): Int {
            return max(0, scrollPane.viewport.width - scrollPane.verticalScrollBar.width)
        }
    }
}
