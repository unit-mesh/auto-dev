package cc.unitmesh.diagram.model

import javax.swing.Icon

/**
 * Base interface for all Graphviz node data types
 * Similar to JdlNodeData in JHipster UML implementation
 */
interface GraphvizNodeData {
    /**
     * Get the name/identifier of this node
     */
    fun getName(): String
    
    /**
     * Get the icon to display for this node
     */
    fun getIcon(): Icon?
}
