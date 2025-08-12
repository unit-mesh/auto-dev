package cc.unitmesh.diagram

import com.intellij.diagram.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.lang.annotations.Pattern
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import com.intellij.diagram.DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER

/**
 * Main provider for Graphviz diagrams
 * Similar to JdlUmlProvider in JHipster UML implementation
 */
class GraphvizUmlProvider : DiagramProvider<GraphvizNodeData>() {
    private val vfsResolver: DiagramVfsResolver<GraphvizNodeData> = GraphvizVfsResolver()
    private val elementManager: DiagramElementManager<GraphvizNodeData> = GraphvizElementManager()

    init {
        (elementManager as GraphvizElementManager).setUmlProvider(this)
    }

    @Pattern("[a-zA-Z0-9_-]*")
    override fun getID(): String {
        return "AutoDev GraphvizDOT"
    }

    override fun getPresentableName(): String {
        return "AutoDev Graphviz DOT Diagram"
    }

    override fun createDataModel(
        project: Project,
        seedData: GraphvizNodeData?,
        umlVirtualFile: VirtualFile?,
        diagramPresentationModel: DiagramPresentationModel
    ): DiagramDataModel<GraphvizNodeData> {
        val model = GraphvizDataModel(project, this, seedData)
        if (seedData != null) {
            model.addElement(seedData)
        }
        return model
    }

    override fun createVisibilityManager(): DiagramVisibilityManager = EmptyDiagramVisibilityManager.INSTANCE

    override fun getElementManager(): DiagramElementManager<GraphvizNodeData> = elementManager

    override fun getVfsResolver(): DiagramVfsResolver<GraphvizNodeData> = vfsResolver

    @Suppress("UNCHECKED_CAST")
    override fun getRelationshipManager(): DiagramRelationshipManager<GraphvizNodeData> {
        return NO_RELATIONSHIP_MANAGER as DiagramRelationshipManager<GraphvizNodeData>
    }

    override fun createNodeContentManager(): DiagramNodeContentManager = GraphvizNodeCategoryManager()
}
