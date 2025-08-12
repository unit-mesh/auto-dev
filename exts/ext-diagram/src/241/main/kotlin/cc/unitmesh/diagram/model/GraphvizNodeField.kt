package cc.unitmesh.diagram.model

/**
 * Represents a field/property of a Graphviz node
 * Similar to JdlEntityNodeField in JHipster UML implementation
 */
data class GraphvizNodeField(
    val name: String,
    val type: String? = null,
    val required: Boolean = false
) {
    

    
    /**
     * Check if this field is required
     */
    fun isRequired(): Boolean = required
    
    override fun toString(): String {
        val typeStr = if (type != null) ": $type" else ""
        val requiredStr = if (required) " (required)" else ""
        return "$name$typeStr$requiredStr"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphvizNodeField) return false
        return name == other.name && type == other.type && required == other.required
    }
    
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + required.hashCode()
        return result
    }
}
