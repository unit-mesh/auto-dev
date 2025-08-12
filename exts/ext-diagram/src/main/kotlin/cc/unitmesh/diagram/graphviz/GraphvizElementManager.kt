package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.AbstractDiagramElementManager
import com.intellij.diagram.DiagramBuilder
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleColoredText
import com.intellij.ui.SimpleTextAttributes
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizDiagramRootData
import cc.unitmesh.diagram.graphviz.model.GraphvizSimpleNodeData
import javax.swing.Icon

/**
 * Element manager for Graphviz diagrams
 * Similar to JdlUmlElementManager in JHipster UML implementation
 */
class GraphvizElementManager : AbstractDiagramElementManager<GraphvizNodeData>() {
    
    private var umlProvider: GraphvizUmlProvider? = null
    
    /**
     * Set the UML provider
     */
    fun setUmlProvider(provider: GraphvizUmlProvider) {
        this.umlProvider = provider
    }
    
    override fun findInDataContext(dataContext: DataContext): GraphvizNodeData? {
        val psiFile = CommonDataKeys.PSI_FILE.getData(dataContext)
        if (psiFile == null) return null
        
        val virtualFile = psiFile.virtualFile
        if (virtualFile == null) return null
        
        // Check if this is a DOT file
        if (!isDotFile(virtualFile)) return null
        
        return getRootData(psiFile.project, virtualFile)
    }
    
    override fun isAcceptableAsNode(element: Any?): Boolean {
        return element is GraphvizNodeData
    }
    
    override fun getElementTitle(element: GraphvizNodeData): String? {
        return element.getName()
    }
    
    override fun getNodeTooltip(element: GraphvizNodeData): String? {
        return when (element) {
            is GraphvizSimpleNodeData -> {
                buildString {
                    append("Node: ${element.getName()}")
                    if (element.getDisplayLabel() != element.getName()) {
                        append("\nLabel: ${element.getDisplayLabel()}")
                    }
                    append("\nShape: ${element.getShape()}")
                    element.getColor()?.let { append("\nColor: $it") }
                    element.getStyle()?.let { append("\nStyle: $it") }
                }
            }
            else -> element.getName()
        }
    }
    
    override fun getItemName(
        nodeElement: GraphvizNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder
    ): SimpleColoredText? {
        // For now, we don't have sub-items in nodes
        // This could be extended to show node attributes as sub-items
        return null
    }

    override fun getItemIcon(
        nodeElement: GraphvizNodeData?,
        nodeItem: Any?,
        builder: DiagramBuilder?
    ): Icon? {
        // For now, we don't have sub-items in nodes
        return null
    }
    
    companion object {
        /**
         * Create root data for a DOT file
         */
        fun getRootData(project: Project, virtualFile: VirtualFile): GraphvizDiagramRootData {
            val disposable = project.getService(GraphvizDiagramService::class.java)
            val filePointer = VirtualFilePointerManager.getInstance()
                .create(virtualFile, disposable, null)
            
            return GraphvizDiagramRootData(filePointer)
        }
        
        /**
         * Check if a file is a DOT file
         */
        private fun isDotFile(virtualFile: VirtualFile): Boolean {
            val extension = virtualFile.extension?.lowercase()
            return extension == "dot" || extension == "gv" || extension == "graphviz"
        }
    }
}
