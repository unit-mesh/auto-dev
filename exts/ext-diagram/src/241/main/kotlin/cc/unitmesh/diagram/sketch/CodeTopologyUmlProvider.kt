package cc.unitmesh.diagram.sketch

import cc.unitmesh.diagram.diagram.CodeTopologyElementManager
import cc.unitmesh.diagram.diagram.CodeTopologyNodeCategoryManager
import cc.unitmesh.diagram.diagram.CodeTopologyVfsResolver
import cc.unitmesh.diagram.diagram.graph.CodeTopologyDataModel
import cc.unitmesh.diagram.model.GraphNodeData
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
class CodeTopologyUmlProvider : DiagramProvider<GraphNodeData>() {
    private val vfsResolver: DiagramVfsResolver<GraphNodeData> = CodeTopologyVfsResolver()
    private val elementManager: DiagramElementManager<GraphNodeData> = CodeTopologyElementManager()

    init {
        (elementManager as CodeTopologyElementManager).setUmlProvider(this)
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
        seedData: GraphNodeData?,
        umlVirtualFile: VirtualFile?,
        diagramPresentationModel: DiagramPresentationModel
    ): DiagramDataModel<GraphNodeData> {
        val model = CodeTopologyDataModel(project, this, seedData)
        if (seedData != null) {
            model.addElement(seedData)
        }

        return model
    }

    override fun createVisibilityManager(): DiagramVisibilityManager = EmptyDiagramVisibilityManager.INSTANCE

    override fun getElementManager(): DiagramElementManager<GraphNodeData> = elementManager

    override fun getVfsResolver(): DiagramVfsResolver<GraphNodeData> = vfsResolver

    @Suppress("UNCHECKED_CAST")
    override fun getRelationshipManager(): DiagramRelationshipManager<GraphNodeData> {
        return DiagramRelationshipManager.NO_RELATIONSHIP_MANAGER as DiagramRelationshipManager<GraphNodeData>
    }

    override fun createNodeContentManager(): DiagramNodeContentManager = CodeTopologyNodeCategoryManager()
}