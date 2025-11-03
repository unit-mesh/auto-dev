package cc.unitmesh.codegraph.model

import kotlinx.serialization.Serializable

/**
 * Represents a complete code graph with nodes and relationships.
 * This is the common model shared across JVM and JS platforms.
 */
@Serializable
data class CodeGraph(
    /**
     * All code nodes in the graph
     */
    val nodes: List<CodeNode>,
    
    /**
     * All relationships between nodes
     */
    val relationships: List<CodeRelationship>,
    
    /**
     * Metadata about the graph
     */
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get a node by its ID
     */
    fun getNodeById(id: String): CodeNode? {
        return nodes.find { it.id == id }
    }
    
    /**
     * Get all nodes of a specific type
     */
    fun getNodesByType(type: CodeElementType): List<CodeNode> {
        return nodes.filter { it.type == type }
    }
    
    /**
     * Get all relationships of a specific type
     */
    fun getRelationshipsByType(type: RelationshipType): List<CodeRelationship> {
        return relationships.filter { it.type == type }
    }
    
    /**
     * Get all outgoing relationships from a node
     */
    fun getOutgoingRelationships(nodeId: String): List<CodeRelationship> {
        return relationships.filter { it.sourceId == nodeId }
    }
    
    /**
     * Get all incoming relationships to a node
     */
    fun getIncomingRelationships(nodeId: String): List<CodeRelationship> {
        return relationships.filter { it.targetId == nodeId }
    }
}

