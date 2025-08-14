package cc.unitmesh.diagram.model

/**
 * Represents a field/property of a Graphviz node
 * Similar to JdlEntityNodeField in JHipster UML implementation
 */
data class GraphNodeField(
    val name: String,
    val type: String? = null,
    val required: Boolean = false,
    val changeStatus: ChangeStatus = ChangeStatus.UNCHANGED,
    val isMethodField: Boolean = false
) {
    


    /**
     * Check if this field is required
     */
    fun isRequired(): Boolean = required

    /**
     * Check if this field is a method
     */
    fun isMethod(): Boolean = isMethodField

    /**
     * Check if this field was added
     */
    fun isAdded(): Boolean = changeStatus.isAdded()

    /**
     * Check if this field was removed
     */
    fun isRemoved(): Boolean = changeStatus.isRemoved()

    /**
     * Check if this field is unchanged
     */
    fun isUnchanged(): Boolean = changeStatus.isUnchanged()

    /**
     * Get the display name with change prefix for diagrams
     */
    fun getDisplayName(): String {
        val prefix = changeStatus.getDisplayPrefix()
        return if (prefix.isNotEmpty()) "$prefix $name" else name
    }
    
    override fun toString(): String {
        val typeStr = if (type != null) ": $type" else ""
        val requiredStr = if (required) " (required)" else ""
        val prefix = changeStatus.getDisplayPrefix()
        val prefixStr = if (prefix.isNotEmpty()) "$prefix " else ""
        return "$prefixStr$name$typeStr$requiredStr"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphNodeField) return false
        return name == other.name && type == other.type && required == other.required &&
               changeStatus == other.changeStatus && isMethodField == other.isMethodField
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + required.hashCode()
        result = 31 * result + changeStatus.hashCode()
        result = 31 * result + isMethodField.hashCode()
        return result
    }
}
