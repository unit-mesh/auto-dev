package cc.unitmesh.diagram.graphviz.editor

import cc.unitmesh.diagram.graphviz.GraphvizDiagramPanel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Preview file editor for Graphviz DOT files
 * Similar to JdlPreviewFileEditor in JHipster UML implementation
 */
class GraphvizPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile
) : UserDataHolderBase(), FileEditor {

    companion object {
        private const val RENDERING_DELAY_MS = 1000
    }

    private val document: Document? = FileDocumentManager.getInstance().getDocument(file)
    private var isDisposed = false

    private val umlPanelWrapper: JPanel = JPanel(BorderLayout())
    private var panel: GraphvizDiagramPanel? = null

    private val mergingUpdateQueue = MergingUpdateQueue("Graphviz", RENDERING_DELAY_MS, true, null, this)
    private val swingAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateUml()
            }
        }, this)
    }

    override fun getComponent(): JComponent = umlPanelWrapper

    override fun getPreferredFocusedComponent(): JComponent? = umlPanelWrapper

    override fun getName(): String = "Graphviz Preview"

    override fun setState(state: FileEditorState) {
        // No state to set
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = !isDisposed

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // No properties to listen to
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // No properties to listen to
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true

        panel?.let { Disposer.dispose(it) }
        panel = null
    }

    override fun getFile(): VirtualFile = file
    fun getProject(): Project = project

    private fun updateUml() {
        if (isDisposed) return

        mergingUpdateQueue.queue(object : Update("update") {
            override fun run() {
                if (isDisposed) return

                swingAlarm.addRequest({
                    if (isDisposed) return@addRequest

                    try {
                        renderUml()
                    } catch (e: Exception) {
                        // Handle rendering errors gracefully
                        e.printStackTrace()
                    }
                }, 0)
            }
        })
    }

    private fun renderUml() {
        if (isDisposed) return

        // Dispose old panel
        panel?.let { Disposer.dispose(it) }

        // Create new panel
        panel = GraphvizDiagramPanel(this)

        umlPanelWrapper.removeAll()
        umlPanelWrapper.add(panel!!.getComponent())

        // Draw the diagram
        panel!!.draw()

        umlPanelWrapper.revalidate()
        umlPanelWrapper.repaint()
    }
}
