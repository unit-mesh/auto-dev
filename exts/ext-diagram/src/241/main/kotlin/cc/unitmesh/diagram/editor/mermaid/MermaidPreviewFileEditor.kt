package cc.unitmesh.diagram.editor.mermaid

import cc.unitmesh.diagram.GraphvizDiagramPanel
import cc.unitmesh.diagram.editor.DiagramPreviewFileEditor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
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
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Preview file editor for Mermaid files
 * Similar to GraphvizPreviewFileEditor but for Mermaid diagrams
 */
class MermaidPreviewFileEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(),
    FileEditor, DiagramPreviewFileEditor {

    companion object {
        private const val RENDERING_DELAY_MS = 1000
    }

    private val document: Document? = FileDocumentManager.getInstance().getDocument(file)
    private var isDisposed = false

    private val umlPanelWrapper: JPanel = JPanel(BorderLayout())

    private var myPanel: GraphvizDiagramPanel? = null

    private val mergingUpdateQueue = MergingUpdateQueue("Mermaid", RENDERING_DELAY_MS, true, null, this)
    private val swingAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateUml()
            }
        }, this)

        umlPanelWrapper.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent?) {
                swingAlarm.addRequest(Runnable {
                    if (myPanel == null) {
                        attachHtmlPanel()
                    }
                }, 0, ModalityState.stateForComponent(getComponent()))
            }

            override fun componentHidden(e: ComponentEvent?) {
                swingAlarm.addRequest(Runnable {
                    if (myPanel != null) {
                        detachHtmlPanel()
                    }
                }, 0, ModalityState.stateForComponent(getComponent()))
            }
        })
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        detachHtmlPanel()
    }

    override fun isValid(): Boolean = !isDisposed

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // No properties to listen to
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // No properties to listen to
    }

    private fun attachHtmlPanel() {
        myPanel = GraphvizDiagramPanel(this as DiagramPreviewFileEditor)
        umlPanelWrapper.add(myPanel!!.getComponent(), BorderLayout.CENTER)
        Disposer.register(this, myPanel!!)

        if (umlPanelWrapper.isShowing()) umlPanelWrapper.validate()
        umlPanelWrapper.repaint()

        myPanel!!.draw()
    }

    private fun detachHtmlPanel() {
        if (myPanel != null) {
            umlPanelWrapper.remove(myPanel!!.getComponent())
            Disposer.dispose(myPanel!!)
            myPanel = null
        }
    }

    override fun getComponent(): JComponent = umlPanelWrapper

    override fun getPreferredFocusedComponent(): JComponent? = myPanel?.getComponent()

    override fun getName(): String = "Mermaid Preview"

    override fun setState(state: FileEditorState) {
        // No state to set
    }

    override fun isModified(): Boolean = false

    override fun selectNotify() {
        if(myPanel == null) {
            attachHtmlPanel()
            return
        }

        myPanel!!.draw()
    }

    override fun deselectNotify() {
        // Keep panel attached for better performance
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getFile(): VirtualFile = file
    override fun getProject(): Project = project

    private fun updateUml() {
        if (myPanel == null || document == null || !file.isValid() || isDisposed) {
            return
        }

        mergingUpdateQueue.queue(object : Update("AUTODEV.UML.UPDATE") {
            override fun run() {
                ApplicationManager.getApplication().invokeLater {
                    myPanel!!.draw()
                }
            }
        })
    }
}
