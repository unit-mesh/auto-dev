package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiModificationTracker
import cc.unitmesh.diagram.graphviz.model.*
import cc.unitmesh.diagram.graphviz.parser.DotFileParser

/**
 * Data model for Graphviz diagrams
 * Similar to JdlUmlDataModel in JHipster UML implementation
 */
class GraphvizDataModel(
    project: Project,
    provider: DiagramProvider<GraphvizNodeData>,
    private val seedData: GraphvizNodeData?
) : DiagramDataModel<GraphvizNodeData>(project, provider) {
    
    private val nodes = mutableListOf<GraphvizDiagramNode>()
    private val edges = mutableListOf<DiagramEdge<GraphvizNodeData>>()
    private var diagramData: GraphvizDiagramData? = null
    
    override fun getModificationTracker(): ModificationTracker {
        // Track modifications to any file in the project
        return PsiModificationTracker.getInstance(project)
    }
    
    override fun getNodes(): Collection<DiagramNode<GraphvizNodeData>> {
        return nodes
    }
    
    override fun getNodeName(diagramNode: DiagramNode<GraphvizNodeData>): String {
        return diagramNode.identifyingElement.getName()
    }
    
    override fun addElement(data: GraphvizNodeData?): DiagramNode<GraphvizNodeData>? {
        if (data == null) return null
        
        if (data is GraphvizDiagramRootData) {
            // Parse the DOT file and create nodes/edges
            this.diagramData = extractData(project, data)
            
            val nodeMapping = mutableMapOf<String, DiagramNode<GraphvizNodeData>>()
            
            // Add all nodes
            diagramData?.nodes?.forEach { nodeData ->
                val diagramNode = addElement(nodeData)
                if (diagramNode != null) {
                    nodeMapping[nodeData.getName()] = diagramNode
                }
            }
            
            // Add all edges
            diagramData?.edges?.forEach { edgeData ->
                val sourceNode = nodeMapping[edgeData.sourceNodeId]
                val targetNode = nodeMapping[edgeData.targetNodeId]
                
                if (sourceNode != null && targetNode != null) {
                    edges.add(GraphvizDiagramEdge(sourceNode, targetNode, edgeData))
                }
            }
            
            return null
        }
        
        // Add individual node
        val node = GraphvizDiagramNode(data, provider)
        nodes.add(node)
        return node
    }
    
    override fun getEdges(): Collection<DiagramEdge<GraphvizNodeData>> {
        return edges
    }
    
    override fun dispose() {
        nodes.clear()
        edges.clear()
        diagramData = null
    }
    
    override fun refreshDataModel() {
        if (seedData is GraphvizDiagramRootData) {
            val newDiagramData = extractData(project, seedData)
            
            if (newDiagramData == diagramData) return // nothing changed
            
            // Clear existing data
            removeAll()
            nodes.clear()
            edges.clear()
            
            // Re-add with new data
            addElement(seedData)
        }
    }
    
    companion object {
        /**
         * Extract diagram data from a DOT file
         */
        fun extractData(project: Project, rootData: GraphvizDiagramRootData): GraphvizDiagramData {
            val virtualFile = rootData.getVirtualFile()
            if (virtualFile == null || !virtualFile.exists()) {
                return GraphvizDiagramData(
                    nodes = emptyList(),
                    edges = emptyList()
                )
            }
            
            return try {
                val content = String(virtualFile.contentsToByteArray())
                val parser = DotFileParser()
                parser.parse(content)
            } catch (e: Exception) {
                // Return empty data if parsing fails
                GraphvizDiagramData(
                    nodes = emptyList(),
                    edges = emptyList()
                )
            }
        }
    }
}
