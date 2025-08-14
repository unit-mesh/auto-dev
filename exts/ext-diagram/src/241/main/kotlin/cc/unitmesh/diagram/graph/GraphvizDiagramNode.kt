package cc.unitmesh.diagram.graph

import com.intellij.diagram.DiagramNodeBase
import com.intellij.diagram.DiagramProvider
import cc.unitmesh.diagram.model.GraphvizNodeData
import javax.swing.Icon

/**
 * Diagram node implementation for Graphviz graphs
 * Similar to JdlDiagramNode in JHipster UML implementation
 */
class GraphvizDiagramNode(
    private val data: GraphvizNodeData,
    provider: DiagramProvider<GraphvizNodeData>
) : DiagramNodeBase<GraphvizNodeData>(provider) {
    
    override fun getIdentifyingElement(): GraphvizNodeData {
        return data
    }
    
    override fun getTooltip(): String? {
        // Could be enhanced to show node attributes or other information
        return data.getName()
    }
    
    override fun getIcon(): Icon? {
        return data.getIcon()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphvizDiagramNode) return false
        return data == other.data
    }
    
    override fun hashCode(): Int {
        return data.hashCode()
    }
    
    override fun toString(): String {
        return "GraphvizDiagramNode(${data.getName()})"
    }
}
