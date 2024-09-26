package com.intellij.temporary.inlay

import cc.unitmesh.devti.gui.quick.QuickPromptField
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayProperties
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.rd.paint2DLine
import com.intellij.temporary.inlay.InlayLayoutManager.Companion.getXOffsetPosition
import com.intellij.ui.JBColor
import com.intellij.ui.paint.LinePainter2D
import com.intellij.util.ui.JBPoint
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel

open class InlayPanel<T : JComponent?>(var component: T) : JPanel() {
    val panel: JPanel
    var inlay: Inlay<*>? = null

    private val visibleAreaListener: VisibleAreaListener

    init {
        panel = object : JPanel() {
            init {
                setOpaque(false)
                setBorder(JBUI.Borders.empty())
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val create: Graphics2D? = g.create() as Graphics2D?
                try {
                    create!!.paint2DLine(
                        JBPoint(0, 0),
                        JBPoint(0, height),
                        LinePainter2D.StrokeType.INSIDE,
                        3.0,
                        JBColor(Color(0, 100, 89, 100), Color(0, 0, 0, 90))
                    )

                    create.dispose()
                } catch (th: Throwable) {
                    create!!.dispose()
                    throw th
                }
            }
        }

        visibleAreaListener = VisibleAreaListener { invalidate() }
    }

    protected open fun setupPane(inlay: Inlay<*>) {
        this.inlay = inlay

        add(panel)
        add(component)
        setOpaque(true)

        inlay.editor.scrollingModel.addVisibleAreaListener(visibleAreaListener, (inlay as Disposable))

        setLayout(object : LayoutManager {
            override fun addLayoutComponent(name: String, comp: Component?) {}
            override fun removeLayoutComponent(comp: Component?) {}
            override fun preferredLayoutSize(parent: Container?): Dimension {
                if (!inlay.isValid || parent == null) return Dimension(0, 0)

                val dimension: Dimension = component!!.preferredSize
                val xOffsetPosition = dimension.width + getXOffsetPosition(inlay)
                val insets = component!!.getInsets()

                return Dimension(xOffsetPosition, dimension.height + insets.height)
            }

            override fun minimumLayoutSize(parent: Container?): Dimension {
                if (!inlay.isValid || parent == null) return Dimension(0, 0)

                val size: Dimension = component!!.getMinimumSize()
                val xOffsetPosition = size.width + getXOffsetPosition(inlay)
                val insets = component!!.getInsets()

                return Dimension(xOffsetPosition, size.height + insets.height)
            }

            override fun layoutContainer(parent: Container?) {
                if (!inlay.isValid) return

                val size = parent?.size ?: Dimension(0, 0)

                val x = getXOffsetPosition(inlay)
                component!!.setBounds(x, 0, size.width - x, size.height)

                val scrollPane = (inlay.editor as EditorEx).scrollPane
                panel.setBounds(scrollPane.viewport.viewRect.x - 1, 0, 5, size.height)
            }
        })
    }

    companion object {
        fun add(editor: EditorEx, offset: Int, component: QuickPromptField): InlayPanel<QuickPromptField>? {
            val properties = InlayProperties().showAbove(false).showWhenFolded(true);

            val inlayPanel = InlayPanel(component)
            val inlayRenderer = InlayRenderer(inlayPanel)

            val inlayElement =
                editor.inlayModel.addBlockElement(offset, properties, inlayRenderer) ?: return null

            ComponentInlaysContainer.addInlay(inlayElement)

            inlayPanel.setupPane(inlayElement)

            return inlayPanel
        }
    }
}