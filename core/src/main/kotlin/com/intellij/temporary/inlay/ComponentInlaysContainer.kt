package com.intellij.temporary.inlay

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Key
import com.intellij.temporary.gui.block.whenDisposed
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.RepaintManager
import javax.swing.SwingUtilities

internal class ComponentInlaysContainer(val editor: Editor) : JComponent() {
    private val inlays: MutableList<Inlay<InlayRenderer>> = mutableListOf()

    val editorResizeListener: ComponentAdapter = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            revalidate()
            repaint()
        }
    }

    fun getInlays(): MutableList<Inlay<InlayRenderer>> = inlays

    override fun invalidate() {
        super.invalidate()
        RepaintManager.currentManager(this).addInvalidComponent(this)
    }

    override fun doLayout() {
        val inlays: List<Inlay<InlayRenderer>> = inlays
        if (inlays.isEmpty()) return

        val content: JComponent = editor.contentComponent
        inlays.forEach {
            it.renderer.inlaySize = it.renderer.component.preferredSize
        }

        ReadAction.run<RuntimeException> {
            editor.inlayModel.execute(true) {
                inlays.forEach {
                    if (it.renderer.inlaySize.width != it.widthInPixels || it.renderer.inlaySize.height != it.heightInPixels) {
                        it.update()
                    }
                }
            }
        }

        if (content.width < content.width) {
            content.parent.doLayout()
        }

        bounds = SwingUtilities.calculateInnerArea(content, null as Rectangle?)
        inlays.forEach {
            it.renderer.component.size = it.renderer.inlaySize
        }
    }

    companion object {
        private val INLAYS_CONTAINER = Key<ComponentInlaysContainer>("INLAYS_CONTAINER")
        fun addInlay(inlay: Inlay<InlayRenderer>) {
            val editor: Editor = inlay.editor
            var component = editor.getUserData(INLAYS_CONTAINER)
            if (component == null) {
                val newContainer = ComponentInlaysContainer(editor)
                editor.putUserData(INLAYS_CONTAINER, newContainer)

                editor.contentComponent.add(newContainer)
                editor.contentComponent.addComponentListener(newContainer.editorResizeListener)

                component = newContainer
            }

            component.getInlays().add(inlay)
            component.add(inlay.renderer.component)


            inlay.whenDisposed {
                if (!component.getInlays().remove(inlay)) {
                    return@whenDisposed
                }
                component.remove(inlay.renderer.component)

                if (component.getInlays().size == 0) {
                    editor.contentComponent.removeComponentListener(component.editorResizeListener)
                    editor.contentComponent.remove(inlay.renderer.component)
                }
            }
        }
    }
}
