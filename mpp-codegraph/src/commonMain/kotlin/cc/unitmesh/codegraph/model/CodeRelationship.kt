package cc.unitmesh.codegraph.model

import kotlinx.serialization.Serializable

/**
 * Represents a relationship between two code nodes.
 * This is the common model shared across JVM and JS platforms.
 */
@Serializable
data class CodeRelationship(
    /**
     * Source node ID
     */
    val sourceId: String,
    
    /**
     * Target node ID
     */
    val targetId: String,
    
    /**
     * Type of relationship
     */
    val type: RelationshipType,
    
    /**
     * Additional metadata about the relationship
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Types of relationships between code elements
 */
@Serializable
enum class RelationshipType {
    /**
     * A contains B (e.g., class contains method)
     */
    MADE_OF,
    
    /**
     * A depends on B (e.g., imports, uses)
     */
    DEPENDS_ON,
    
    /**
     * A extends B (inheritance)
     */
    EXTENDS,
    
    /**
     * A implements B (interface implementation)
     */
    IMPLEMENTS,
    
    /**
     * A calls B (method invocation)
     */
    CALLS,
    
    /**
     * A references B (field/variable reference)
     */
    REFERENCES,
    
    /**
     * A overrides B (method override)
     */
    OVERRIDES,
    
    /**
     * Unknown relationship type
     */
    UNKNOWN
}

