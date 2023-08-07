// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package cc.unitmesh.devti.editor.inlay

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.geom.Rectangle2D
import javax.swing.JComponent
import javax.swing.JScrollPane
import kotlin.math.max
import kotlin.math.min
import com.intellij.temporary.gui.block.whenDisposed

class InlayComponent<T : JComponent?> private constructor(@JvmField var component: T) : JComponent(),
    EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return this.maximumWidth
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return this.component!!.preferredHeight
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        if (bounds != inlay.bounds && inlay.bounds != null) {
            bounds = inlay.bounds!!
            this.component!!.size = bounds.size
        }
    }

    override fun paint(inlay: Inlay<*>, g: Graphics2D, targetRegion: Rectangle2D, textAttributes: TextAttributes) {
        super<EditorCustomElementRenderer>.paint(inlay, g, targetRegion, textAttributes)
    }

    fun updateWidth(maxWidth: Int) {
        this.maximumWidth = maxWidth
        this.component!!.size = Dimension(min(this.component!!.preferredWidth, maxWidth), this.component!!.height)
    }

    companion object {
        fun <T : JComponent?> add(
            editor: Editor,
            offset: Int,
            properties: InlayProperties,
            compo: T
        ): Inlay<InlayComponent<T>>? {
            val scrollPane = (editor as EditorEx).scrollPane
            val inlayComponent: InlayComponent<T> = InlayComponent(compo)
            inlayComponent.updateWidth(calculateMaxWidth(scrollPane))
            val addBlockElement =
                editor.inlayModel.addBlockElement(offset, properties, inlayComponent as EditorCustomElementRenderer)
                    ?: return null

            inlayComponent.add(compo as Component)
            editor.contentComponent.add(inlayComponent)
            val componentListener: ComponentListener = object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    val calculateMaxWidth: Int = calculateMaxWidth(scrollPane)
                    inlayComponent.updateWidth(calculateMaxWidth)
                    addBlockElement.update()
                }
            }

            val viewport = scrollPane.viewport
            viewport.addComponentListener(componentListener)

            addBlockElement.whenDisposed {
                editor.getContentComponent().remove(inlayComponent)
                viewport.removeComponentListener(componentListener)
            }

            return addBlockElement as Inlay<InlayComponent<T>>
        }

        fun calculateMaxWidth(scrollPane: JScrollPane): Int {
            return max(0, scrollPane.viewport.width - scrollPane.verticalScrollBar.width)
        }
    }
}
