package com.intellij.temporary.inlay

import cc.unitmesh.devti.actions.quick.QuickPrompt
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.rd.paint2DLine
import com.intellij.ui.JBColor
import com.intellij.ui.paint.LinePainter2D
import com.intellij.util.ui.JBPoint
import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.Border

open class InlayPanel<T : JComponent?>(var component: T) : JPanel() {
    val panel: JPanel
    var inlay: Inlay<*>? = null

    private val visibleAreaListener: VisibleAreaListener

    init {
        panel = object : JPanel() {
            init {
                setOpaque(false)
                setBorder(JBUI.Borders.empty() as Border)
            }

            override fun paintComponent(g: Graphics?) {
                super.paintComponent(g)
                val create: Graphics2D? = g?.create() as Graphics2D?
                try {
                    create!!.paint2DLine(
                        JBPoint(0, 0),
                        JBPoint(0, height),
                        LinePainter2D.StrokeType.INSIDE,
                        3.0,
                        JBColor(10582004, 5782906)
                    )

                    create.dispose()
                } catch (th: Throwable) {
                    create!!.dispose()
                    throw th
                }
            }
        }

        visibleAreaListener =
            VisibleAreaListener { v1: VisibleAreaEvent? ->
                if (v1 != null) {
                    invalidate()
                }
            }
    }

    protected open fun setupPane(inlay: Inlay<*>) {
        this.inlay = inlay

        add(panel)
        add(component)
        setOpaque(true)
        setBackground(JBColor(16446975, 16446975))

        inlay.editor.scrollingModel.addVisibleAreaListener(visibleAreaListener, (inlay as Disposable))

        setLayout(InlayLayoutManager(inlay))
    }

    companion object {
        fun add(editor: EditorEx, offset: Int, component: QuickPrompt): InlayPanel<QuickPrompt>? {
            val inlayPanel = InlayPanel(component)

            val properties = InlayProperties().showAbove(true)
                .showWhenFolded(true);

            val inlayComponent = InlayComponent(component)
            inlayComponent.updateWidth(InlayComponent.calculateMaxWidth(editor.scrollPane))

            val inlayElement =
                editor.inlayModel.addBlockElement(offset, properties, inlayComponent) ?: return null

            inlayPanel.setupPane(inlayElement)
            editor.contentComponent.add(inlayPanel)

            return inlayPanel
        }
    }
}