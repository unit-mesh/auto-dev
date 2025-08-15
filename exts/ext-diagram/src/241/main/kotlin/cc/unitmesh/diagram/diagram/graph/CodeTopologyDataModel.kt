package cc.unitmesh.diagram.diagram.graph

import com.intellij.diagram.DiagramDataModel
import com.intellij.diagram.DiagramEdge
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.PsiModificationTracker
import cc.unitmesh.diagram.model.*
import cc.unitmesh.diagram.parser.DotFileParser

/**
 * Data model for Graphviz diagrams
 * Similar to JdlUmlDataModel in JHipster UML implementation
 */
class CodeTopologyDataModel(
    project: Project,
    provider: DiagramProvider<GraphNodeData>,
    private val seedData: GraphNodeData?
) : DiagramDataModel<GraphNodeData>(project, provider) {

    private val nodes = mutableListOf<CodeTopologyDiagramNode>()
    private val edges = mutableListOf<DiagramEdge<GraphNodeData>>()
    private var diagramData: GraphDiagramData? = null

    override fun getModificationTracker(): ModificationTracker {
        return PsiModificationTracker.getInstance(project)
    }

    override fun getNodes(): Collection<DiagramNode<GraphNodeData>> {
        return nodes
    }

    override fun getNodeName(diagramNode: DiagramNode<GraphNodeData>): String {
        return diagramNode.identifyingElement.getName()
    }

    override fun addElement(data: GraphNodeData?): DiagramNode<GraphNodeData>? {
        if (data == null) return null

        if (data is GraphDiagramRootData) {
            // Parse the DOT file and create nodes/edges
            this.diagramData = extractData(project, data)

            val nodeMapping = mutableMapOf<String, DiagramNode<GraphNodeData>>()

            // Add all nodes
            diagramData?.nodes?.forEach { nodeData ->
                val diagramNode = addElement(nodeData)
                if (diagramNode != null) {
                    nodeMapping[nodeData.getName()] = diagramNode
                }
            }

            // Add all entities
            diagramData?.entities?.forEach { entityData ->
                val diagramNode = addElement(entityData)
                if (diagramNode != null) {
                    nodeMapping[entityData.getName()] = diagramNode
                }
            }

            // Add all subgraphs as nodes
            diagramData?.subgraphs?.forEach { subgraphData ->
                val subgraphNode = GraphSimpleNodeData(
                    id = subgraphData.name,
                    label = subgraphData.getDisplayLabel(),
                    attributes = subgraphData.attributes,
                    nodeType = GraphvizNodeType.CLUSTER
                )
                val diagramNode = addElement(subgraphNode)
                if (diagramNode != null) {
                    nodeMapping[subgraphData.name] = diagramNode
                }
            }

            // Add all edges
            diagramData?.edges?.forEach { edgeData ->
                val sourceNode = nodeMapping[edgeData.sourceNodeId]
                val targetNode = nodeMapping[edgeData.targetNodeId]

                if (sourceNode != null && targetNode != null) {
                    edges.add(CodeTopologyEntityEdge(sourceNode, targetNode, edgeData))
                }
            }

            // Add edges from subgraphs
            diagramData?.subgraphs?.forEach { subgraphData ->
                subgraphData.edges.forEach { edgeData ->
                    val sourceNode = nodeMapping[edgeData.sourceNodeId]
                    val targetNode = nodeMapping[edgeData.targetNodeId]

                    if (sourceNode != null && targetNode != null) {
                        edges.add(CodeTopologyEntityEdge(sourceNode, targetNode, edgeData))
                    }
                }
            }

            return null
        }

        val node = CodeTopologyDiagramNode(data, provider)
        nodes.add(node)
        return node
    }

    override fun getEdges(): Collection<DiagramEdge<GraphNodeData>> {
        return edges
    }

    override fun dispose() {
        nodes.clear()
        edges.clear()
        diagramData = null
    }

    override fun refreshDataModel() {
        if (seedData is GraphDiagramRootData) {
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

    companion object Companion {
        /**
         * Extract diagram data from a DOT file
         */
        fun extractData(project: Project, rootData: GraphDiagramRootData): GraphDiagramData {
            val virtualFile = rootData.getVirtualFile()
            if (virtualFile == null || !virtualFile.exists()) {
                return GraphDiagramData(
                    nodes = emptyList(),
                    entities = emptyList(),
                    edges = emptyList(),
                    subgraphs = emptyList()
                )
            }

            return try {
                val content = String(virtualFile.contentsToByteArray())
                val parser = DotFileParser()
                parser.parse(content)
            } catch (e: Exception) {
                // Return empty data if parsing fails
                GraphDiagramData(
                    nodes = emptyList(),
                    entities = emptyList(),
                    edges = emptyList(),
                    subgraphs = emptyList()
                )
            }
        }
    }
}
