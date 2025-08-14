package cc.unitmesh.diagram.model

/**
 * Represents an edge/connection between two nodes in a Graphviz graph
 */
data class GraphEdgeData(
    val sourceNodeId: String,
    val targetNodeId: String,
    val label: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val edgeType: GraphvizEdgeType = GraphvizEdgeType.DIRECTED
) {
    
    /**
     * Get a specific attribute value
     */
    fun getAttribute(key: String): String? = attributes[key]
    
    /**
     * Get the color of the edge
     */
    fun getColor(): String? = getAttribute("color")
    
    /**
     * Get the style of the edge
     */
    fun getStyle(): String? = getAttribute("style")
    
    /**
     * Get the arrow head type
     */
    fun getArrowHead(): String? = getAttribute("arrowhead")
    
    /**
     * Get the arrow tail type
     */
    fun getArrowTail(): String? = getAttribute("arrowtail")
    
    /**
     * Check if this edge has a specific attribute
     */
    fun hasAttribute(key: String): Boolean = attributes.containsKey(key)
    
    override fun toString(): String {
        val arrow = if (edgeType == GraphvizEdgeType.DIRECTED) "->" else "--"
        return "GraphvizEdgeData($sourceNodeId $arrow $targetNodeId${if (label != null) " [$label]" else ""})"
    }
}

/**
 * Types of Graphviz edges
 */
enum class GraphvizEdgeType {
    DIRECTED,    // Directed edge (->)
    UNDIRECTED   // Undirected edge (--)
}
