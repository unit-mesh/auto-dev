package cc.unitmesh.diagram.model

/**
 * Represents a subgraph/cluster in a Graphviz graph
 */
data class GraphSubgraphData(
    val name: String,
    val label: String? = null,
    val nodes: Collection<String> = emptyList(),
    val edges: Collection<GraphEdgeData> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
    val isCluster: Boolean = false
) {
    
    /**
     * Get a specific attribute value
     */
    fun getAttribute(key: String): String? = attributes[key]
    
    /**
     * Get the color of the subgraph
     */
    fun getColor(): String? = getAttribute("color")
    
    /**
     * Get the style of the subgraph
     */
    fun getStyle(): String? = getAttribute("style")
    
    /**
     * Get the background color of the subgraph
     */
    fun getBgColor(): String? = getAttribute("bgcolor")
    
    /**
     * Get the display label for this subgraph
     */
    fun getDisplayLabel(): String = label ?: name
    
    /**
     * Check if this subgraph has a specific attribute
     */
    fun hasAttribute(key: String): Boolean = attributes.containsKey(key)
    
    /**
     * Check if this subgraph contains a specific node
     */
    fun containsNode(nodeId: String): Boolean = nodes.contains(nodeId)
    
    override fun toString(): String {
        val type = if (isCluster) "cluster" else "subgraph"
        return "GraphSubgraphData($type='$name', label='$label', nodes=${nodes.size}, edges=${edges.size})"
    }
}
