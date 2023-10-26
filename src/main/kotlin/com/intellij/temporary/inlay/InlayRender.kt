package com.intellij.temporary.inlay

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JComponent

class InlayRender<T : JComponent>(var component: T) : EditorCustomElementRenderer {
    private var inlaySize: Dimension = Dimension(0, 0)

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        return inlaySize.width
    }

    override fun calcHeightInPixels(inlay: Inlay<*>): Int {
        return inlaySize.height
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val bounds = inlay.bounds ?: return
        val inlayLocation: Point = bounds.location ?: return

        if (component.location != inlayLocation) {
            component.location = inlayLocation
        }
    }
}
