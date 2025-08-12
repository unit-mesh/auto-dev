package cc.unitmesh.diagram.model

/**
 * Container for all diagram data extracted from a DOT file
 * Similar to JdlDiagramData in JHipster UML implementation
 */
data class GraphvizDiagramData(
    val nodes: Collection<GraphvizSimpleNodeData>,
    val entities: Collection<GraphvizEntityNodeData> = emptyList(),
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
     * Get an entity by name
     */
    fun getEntityByName(name: String): GraphvizEntityNodeData? {
        return entities.find { it.getName() == name }
    }

    /**
     * Check if the graph is empty
     */
    fun isEmpty(): Boolean = nodes.isEmpty() && entities.isEmpty() && edges.isEmpty()
    
    override fun toString(): String {
        return "GraphvizDiagramData(nodes=${nodes.size}, entities=${entities.size}, edges=${edges.size}, type=$graphType)"
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
