package cc.unitmesh.diagram.model

import com.intellij.util.PlatformIcons
import javax.swing.Icon

/**
 * Simple implementation of GraphvizNodeData for regular graph nodes
 * Represents a single node in the Graphviz graph
 */
class GraphSimpleNodeData(
    private val id: String,
    private val label: String? = null,
    private val attributes: Map<String, String> = emptyMap(),
    private val nodeType: GraphvizNodeType = GraphvizNodeType.REGULAR,
    private val fields: List<GraphNodeField> = emptyList()
) : GraphNodeData {
    
    override fun getName(): String = id
    
    /**
     * Get the display label for this node
     */
    fun getDisplayLabel(): String = label ?: id
    
    /**
     * Get node attributes
     */
    fun getAttributes(): Map<String, String> = attributes
    
    /**
     * Get a specific attribute value
     */
    fun getAttribute(key: String): String? = attributes[key]
    
    /**
     * Get the shape of the node
     */
    fun getShape(): String = getAttribute("shape") ?: "ellipse"
    
    /**
     * Get the color of the node
     */
    fun getColor(): String? = getAttribute("color")
    
    /**
     * Get the style of the node
     */
    fun getStyle(): String? = getAttribute("style")
    
    /**
     * Get the node type
     */
    fun getNodeType(): GraphvizNodeType = nodeType

    /**
     * Get node fields/properties
     */
    fun getFields(): List<GraphNodeField> = fields

    /**
     * Check if this node has fields
     */
    fun hasFields(): Boolean = fields.isNotEmpty()
    
    override fun getIcon(): Icon? {
        return when (nodeType) {
            GraphvizNodeType.REGULAR -> PlatformIcons.CLASS_ICON
            GraphvizNodeType.CLUSTER -> PlatformIcons.PACKAGE_ICON
            GraphvizNodeType.RECORD -> PlatformIcons.INTERFACE_ICON
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphSimpleNodeData) return false
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id.hashCode()
    }
    
    override fun toString(): String {
        return "GraphvizSimpleNodeData(id='$id', label='$label', type=$nodeType)"
    }
}

/**
 * Types of Graphviz nodes
 */
enum class GraphvizNodeType {
    REGULAR,    // Regular node
    CLUSTER,    // Cluster/subgraph
    RECORD      // Record-shaped node
}
