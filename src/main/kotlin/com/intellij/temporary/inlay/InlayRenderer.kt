package com.intellij.temporary.inlay

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.*

class InlayRenderer(var component: Component) : EditorCustomElementRenderer {
    var inlaySize: Dimension = Dimension(0, 0)

    override fun calcWidthInPixels(inlay: Inlay<*>): Int = inlaySize.width

    override fun calcHeightInPixels(inlay: Inlay<*>): Int = inlaySize.height

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val bounds = inlay.bounds ?: return
        val inlayLocation: Point = bounds.location ?: return

        if (component.location != inlayLocation) {
            component.location = inlayLocation
        }
    }
}
