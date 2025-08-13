package cc.unitmesh.diagram.model

/**
 * Represents the change status of a code structure element
 * Used to track additions, deletions, and unchanged elements in AI-generated code comparisons
 */
enum class ChangeStatus {
    /**
     * Element was added in the new version
     */
    ADDED,
    
    /**
     * Element was removed from the old version
     */
    REMOVED,
    
    /**
     * Element exists in both versions (unchanged)
     */
    UNCHANGED;
    
    /**
     * Check if this element was added
     */
    fun isAdded(): Boolean = this == ADDED
    
    /**
     * Check if this element was removed
     */
    fun isRemoved(): Boolean = this == REMOVED
    
    /**
     * Check if this element is unchanged
     */
    fun isUnchanged(): Boolean = this == UNCHANGED
    
    /**
     * Get the display prefix for this change status
     * Used in Mermaid diagrams to show + for added, - for removed
     */
    fun getDisplayPrefix(): String = when (this) {
        ADDED -> "+"
        REMOVED -> "-"
        UNCHANGED -> ""
    }
}
