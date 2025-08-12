package cc.unitmesh.diagram.model

import com.intellij.util.PlatformIcons
import javax.swing.Icon

/**
 * Represents an entity node in a Graphviz graph with fields/properties
 * Similar to JdlEntityNodeData in JHipster UML implementation
 */
class GraphvizEntityNodeData(
    private val name: String,
    private val fields: List<GraphvizNodeField>
) : GraphvizNodeData {
    
    override fun getName(): String = name
    
    override fun getIcon(): Icon = PlatformIcons.CLASS_ICON
    
    /**
     * Get the fields/properties of this entity
     */
    fun getFields(): List<GraphvizNodeField> = fields
    
    /**
     * Check if this entity has fields
     */
    fun hasFields(): Boolean = fields.isNotEmpty()
    
    /**
     * Get a field by name
     */
    fun getField(fieldName: String): GraphvizNodeField? {
        return fields.find { it.name == fieldName }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphvizEntityNodeData) return false
        return name == other.name && fields == other.fields
    }
    
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fields.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "GraphvizEntityNodeData(name='$name', fields=${fields.size})"
    }
}
