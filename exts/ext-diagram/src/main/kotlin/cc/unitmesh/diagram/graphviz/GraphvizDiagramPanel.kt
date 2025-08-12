package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramBuilderFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.graph.services.GraphLayoutService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.JBColor
import com.intellij.uml.components.UmlGraphZoomableViewport
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel for displaying Graphviz diagrams
 * Similar to JdlDiagramPanel in JHipster UML implementation
 */
class GraphvizDiagramPanel(
    private val fileEditor: GraphvizPreviewFileEditor
) : Disposable {
    
    private var builder: DiagramBuilder? = null
    private val chartPanel = MyPanel()
    private val umlProvider = GraphvizUmlProvider()
    
    override fun dispose() {
        // Cleanup is handled by Disposer
    }
    
    fun getComponent(): JComponent {
        return chartPanel
    }
    
    fun draw() {
        if (builder == null) {
            val project = fileEditor.getProject()
            val virtualFile = fileEditor.getFile()
            
            builder = DiagramBuilderFactory.getInstance()
                .create(project, umlProvider, GraphvizElementManager.getRootData(project, virtualFile), null)
            
            Disposer.register(this, builder!!)
            builder!!.view.setFitContentOnResize(true)
            
            val graphView = createSimpleGraphView(builder!!)
            chartPanel.add(graphView, BorderLayout.CENTER)
            chartPanel.revalidate()
            chartPanel.repaint()
        }
    }
    
    private fun createSimpleGraphView(builder: DiagramBuilder): JComponent {
        builder.presentationModel.registerActions()
        val view = builder.view
        view.canvasComponent.background = JBColor.GRAY
        
        // Apply graph layout
        GraphLayoutService.getInstance()
            .queryLayout(builder.graphBuilder)
            .withFitContent(GraphLayoutService.GraphLayoutQueryParams.FitContentOption.AFTER)
            .run()
        
        return UmlGraphZoomableViewport(builder)
    }
    
    private inner class MyPanel : JPanel(BorderLayout()) {
        init {
            // Set up data context for the panel
            DataManager.registerDataProvider(this) { dataId ->
                when (dataId) {
                    CommonDataKeys.VIRTUAL_FILE.name -> fileEditor.getFile()
                    CommonDataKeys.PSI_FILE.name -> PsiManager.getInstance(fileEditor.getProject()).findFile(fileEditor.getFile())
                    else -> null
                }
            }
        }
    }
}

/**
 * Interface for file editor that can provide project and file information
 */
interface GraphvizPreviewFileEditor {
    fun getProject(): Project
    fun getFile(): VirtualFile
}
