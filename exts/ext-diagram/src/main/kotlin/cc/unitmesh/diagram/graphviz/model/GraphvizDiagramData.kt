package cc.unitmesh.diagram.graphviz.model

/**
 * Container for all diagram data extracted from a DOT file
 * Similar to JdlDiagramData in JHipster UML implementation
 */
data class GraphvizDiagramData(
    val nodes: Collection<GraphvizSimpleNodeData>,
    val edges: Collection<GraphvizEdgeData>,
    val graphAttributes: Map<String, String> = emptyMap(),
    val graphType: GraphvizGraphType = GraphvizGraphType.DIGRAPH
) {
    
    /**
     * Get a node by its ID
     */
    fun getNodeById(id: String): GraphvizSimpleNodeData? {
        return nodes.find { it.getName() == id }
    }
    
    /**
     * Get all edges that start from a specific node
     */
    fun getEdgesFromNode(nodeId: String): List<GraphvizEdgeData> {
        return edges.filter { it.sourceNodeId == nodeId }
    }
    
    /**
     * Get all edges that end at a specific node
     */
    fun getEdgesToNode(nodeId: String): List<GraphvizEdgeData> {
        return edges.filter { it.targetNodeId == nodeId }
    }
    
    /**
     * Get a graph attribute value
     */
    fun getGraphAttribute(key: String): String? = graphAttributes[key]
    
    /**
     * Check if the graph is empty
     */
    fun isEmpty(): Boolean = nodes.isEmpty() && edges.isEmpty()
    
    override fun toString(): String {
        return "GraphvizDiagramData(nodes=${nodes.size}, edges=${edges.size}, type=$graphType)"
    }
}

/**
 * Types of Graphviz graphs
 */
enum class GraphvizGraphType {
    GRAPH,      // Undirected graph
    DIGRAPH,    // Directed graph
    SUBGRAPH    // Subgraph
}
