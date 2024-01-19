// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.FocusChangeListener
import com.intellij.openapi.ui.ErrorBorderCapable
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ObjectUtils
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.MacUIUtil
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import javax.swing.JComponent
import javax.swing.border.Border

class AutoDevCoolBorder(private val editor: EditorEx, parent: JComponent) : Border, ErrorBorderCapable {
    init {
        editor.addFocusListener(object : FocusChangeListener {
            override fun focusGained(editor2: Editor) {
                parent.repaint()
            }

            override fun focusLost(editor2: Editor) {
                parent.repaint()
            }
        })
    }

    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val hasFocus = editor.contentComponent.hasFocus()
        val r = Rectangle(x, y, width, height)
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                if (MacUIUtil.USE_QUARTZ) RenderingHints.VALUE_STROKE_PURE else RenderingHints.VALUE_STROKE_NORMALIZE
            )
            JBInsets.removeFrom(r, JBUI.insets(1))
            g2.translate(r.x, r.y)
            val bw = DarculaUIUtil.BW.float
            val shape: Shape = Rectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2)
            g2.color = c.getBackground()
            g2.fill(shape)
            if (editor.contentComponent.isEnabled && editor.contentComponent.isVisible) {
                val op = getOutline((c as JComponent))
                if (op == null) {
                    g2.color = if (hasFocus) JBUI.CurrentTheme.Focus.focusColor() else DarculaUIUtil.getOutlineColor(
                        editor.contentComponent.isEnabled,
                        false
                    )
                } else {
                    op.setGraphicsColor(g2, hasFocus)
                }
                doPaint(g2, r.width, r.height, 5.0f, if (hasFocus) 1.0f else 0.5f, true)
            }
        } finally {
            g2.dispose()
        }
    }

    override fun getBorderInsets(c: Component): Insets {
        return JBInsets.create(6, 8).asUIResource()
    }

    override fun isBorderOpaque(): Boolean {
        return true
    }
}

fun getOutline(component: JComponent): DarculaUIUtil.Outline? {
    val outline = ObjectUtils.tryCast(component.getClientProperty("JComponent.outline"), String::class.java)
    return if (outline == null) null else DarculaUIUtil.Outline.valueOf(outline)
}

/// for 222 version
fun doPaint(g: Graphics2D, width: Int, height: Int, arc: Float, bw: Float, symmetric: Boolean) {
    var bw = bw

    val f = if (UIUtil.isRetina(g)) 0.5f else 1.0f
    val lw = if (UIUtil.isUnderDefaultMacTheme()) JBUIScale.scale(f) else DarculaUIUtil.LW.float

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(
        RenderingHints.KEY_STROKE_CONTROL,
        if (MacUIUtil.USE_QUARTZ) RenderingHints.VALUE_STROKE_PURE else RenderingHints.VALUE_STROKE_NORMALIZE
    )
    val outerArc = if (arc > 0) arc + bw - JBUIScale.scale(2f) else bw
    val rightOuterArc = if (symmetric) outerArc else JBUIScale.scale(6f)
    val outerRect: Path2D = Path2D.Float(Path2D.WIND_EVEN_ODD)
    outerRect.moveTo((width - rightOuterArc).toDouble(), 0.0)
    outerRect.quadTo(width.toDouble(), 0.0, width.toDouble(), rightOuterArc.toDouble())
    outerRect.lineTo(width.toDouble(), (height - rightOuterArc).toDouble())
    outerRect.quadTo(width.toDouble(), height.toDouble(), (width - rightOuterArc).toDouble(), height.toDouble())
    outerRect.lineTo(outerArc.toDouble(), height.toDouble())
    outerRect.quadTo(0.0, height.toDouble(), 0.0, (height - outerArc).toDouble())
    outerRect.lineTo(0.0, outerArc.toDouble())
    outerRect.quadTo(0.0, 0.0, outerArc.toDouble(), 0.0)
    outerRect.closePath()
    bw += lw
    val rightInnerArc = if (symmetric) outerArc else JBUIScale.scale(7f)
    val innerRect: Path2D = Path2D.Float(Path2D.WIND_EVEN_ODD)
    innerRect.moveTo((width - rightInnerArc).toDouble(), bw.toDouble())
    innerRect.quadTo((width - bw).toDouble(), bw.toDouble(), (width - bw).toDouble(), rightInnerArc.toDouble())
    innerRect.lineTo((width - bw).toDouble(), (height - rightInnerArc).toDouble())
    innerRect.quadTo(
        (width - bw).toDouble(),
        (height - bw).toDouble(),
        (width - rightInnerArc).toDouble(),
        (height - bw).toDouble()
    )
    innerRect.lineTo(outerArc.toDouble(), (height - bw).toDouble())
    innerRect.quadTo(bw.toDouble(), (height - bw).toDouble(), bw.toDouble(), (height - outerArc).toDouble())
    innerRect.lineTo(bw.toDouble(), outerArc.toDouble())
    innerRect.quadTo(bw.toDouble(), bw.toDouble(), outerArc.toDouble(), bw.toDouble())
    innerRect.closePath()
    val path: Path2D = Path2D.Float(Path2D.WIND_EVEN_ODD)
    path.append(outerRect, false)
    path.append(innerRect, false)
    g.fill(path)
}