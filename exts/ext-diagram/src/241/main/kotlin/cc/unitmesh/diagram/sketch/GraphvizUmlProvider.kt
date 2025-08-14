package cc.unitmesh.diagram.sketch

import cc.unitmesh.diagram.diagram.GraphvizElementManager
import cc.unitmesh.diagram.diagram.GraphvizNodeCategoryManager
import cc.unitmesh.diagram.diagram.GraphvizVfsResolver
import cc.unitmesh.diagram.diagram.graph.GraphvizDataModel
import cc.unitmesh.diagram.model.GraphvizNodeData
import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramElementManager
import com.intellij.diagram.DiagramNodeContentManager
import com.intellij.diagram.DiagramPresentationModel
import com.intellij.diagram.DiagramProvider
import com.intellij.diagram.DiagramRelationshipManager
import com.intellij.diagram.DiagramVfsResolver
import com.intellij.diagram.DiagramVisibilityManager
import com.intellij.diagram.EmptyDiagramVisibilityManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.intellij.lang.annotations.Pattern

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
        return DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER as DiagramRelationshipManager<GraphvizNodeData>
    }

    override fun createNodeContentManager(): DiagramNodeContentManager = GraphvizNodeCategoryManager()
}