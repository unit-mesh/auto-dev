package cc.unitmesh.diagram.model

/**
 * Represents a node attribute as a diagram item
 * Similar to JdlEnumNodeItem in JHipster UML implementation
 */
data class GraphAttributeItem(
    val key: String,
    val value: String
) {
    override fun toString(): String {
        return "$key = $value"
    }
}
