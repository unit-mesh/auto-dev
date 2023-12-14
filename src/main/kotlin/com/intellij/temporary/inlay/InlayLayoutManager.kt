package com.intellij.temporary.inlay

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Inlay
import com.intellij.util.DocumentUtil
import com.intellij.util.text.CharArrayUtil
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.LayoutManager
import javax.swing.JComponent
import kotlin.math.max

class InlayLayoutManager(val inlay: Inlay<*>) : LayoutManager {
    private val gap: Int = JBUI.scale(0)

    override fun addLayoutComponent(name: String?, comp: Component?) {}
    override fun removeLayoutComponent(comp: Component?) {}

    override fun preferredLayoutSize(parent: Container?): Dimension {
        if (!inlay.isValid || parent == null) return Dimension(0, 0)

        var width = 0
        var height = 0

        parent.components.forEachIndexed { index, item ->
            val jComponent = item as? JComponent ?: error("Assertion failed")
            val x = getXOffsetPosition(inlay)

            if (index > 0) {
                height += gap
            }

            val it = jComponent.preferredSize
            height += it.height
            width = max(width, x + it.width)
        }

        return Dimension(width, height)
    }

    override fun minimumLayoutSize(parent: Container?): Dimension {
        if (!inlay.isValid || parent == null) return Dimension(0, 0)

        var width = 0
        var height = 0

        parent.components.forEachIndexed { index, item ->
            val jComponent = item as? JComponent ?: error("Assertion failed")
            val x = getXOffsetPosition(inlay)

            if (index > 0) {
                height += gap
            }

            val minimumSize = jComponent.minimumSize
            height += minimumSize.height
            width = max(width, x + minimumSize.width)
        }

        return Dimension(width, height)
    }


    override fun layoutContainer(parent: Container?) {
        if (!inlay.isValid) return

        val x = getXOffsetPosition(inlay)
        var y = 0

        if (parent == null) return

        parent.components.forEach { component ->
            val ps = component.preferredSize
            component.setBounds(x, y, ps.width, ps.height)
            y += gap
        }
    }


    companion object {
        fun getXOffsetPosition(inlay: Inlay<*>): Int {
            if (!inlay.isValid) return 0

            val editor = inlay.editor
            val compute = ReadAction.compute<Int, RuntimeException> {
                val lineStartOffset = DocumentUtil.getLineStartOffset(inlay.offset, editor.document)
                val shiftForward =
                    CharArrayUtil.shiftForward(editor.document.immutableCharSequence, lineStartOffset, " \t")
                editor.offsetToXY(shiftForward).x
            }

            return compute
        }
    }

}
