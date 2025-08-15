package cc.unitmesh.diagram.model

/**
 * Container for all diagram data extracted from a DOT file
 * Similar to JdlDiagramData in JHipster UML implementation
 */
data class GraphDiagramData(
    val nodes: Collection<GraphSimpleNodeData>,
    val entities: Collection<GraphEntityNodeData> = emptyList(),
    val edges: Collection<GraphEdgeData>,
    val graphAttributes: Map<String, String> = emptyMap(),
    val graphType: GraphGraphType = GraphGraphType.DIGRAPH
) {
    fun getNodeById(id: String): GraphSimpleNodeData? {
        return nodes.find { it.getName() == id }
    }

    fun getEdgesFromNode(nodeId: String): List<GraphEdgeData> {
        return edges.filter { it.sourceNodeId == nodeId }
    }

    fun getEdgesToNode(nodeId: String): List<GraphEdgeData> {
        return edges.filter { it.targetNodeId == nodeId }
    }

    fun getGraphAttribute(key: String): String? = graphAttributes[key]
    
    fun getEntityByName(name: String): GraphEntityNodeData? {
        return entities.find { it.getName() == name }
    }

    fun isEmpty(): Boolean = nodes.isEmpty() && entities.isEmpty() && edges.isEmpty()
    
    override fun toString(): String {
        return "GraphvizDiagramData(nodes=${nodes.size}, entities=${entities.size}, edges=${edges.size}, type=$graphType)"
    }
}

/**
 * Types of Graphviz graphs
 */
enum class GraphGraphType {
    GRAPH,      // Undirected graph
    DIGRAPH,    // Directed graph
    SUBGRAPH    // Subgraph
}
