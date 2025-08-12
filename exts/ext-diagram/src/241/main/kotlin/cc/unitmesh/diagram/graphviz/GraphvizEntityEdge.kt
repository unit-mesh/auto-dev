package cc.unitmesh.diagram.graphviz

import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeData
import cc.unitmesh.diagram.graphviz.model.GraphvizEdgeData
import cc.unitmesh.diagram.graphviz.model.GraphvizEdgeType

/**
 * Diagram edge implementation for Graphviz graphs
 * Similar to JdlDiagramEntityEdge in JHipster UML implementation
 */
class GraphvizEntityEdge(
    source: DiagramNode<GraphvizNodeData>,
    target: DiagramNode<GraphvizNodeData>,
    private val edgeData: GraphvizEdgeData
) : DiagramEdgeBase<GraphvizNodeData>(source, target, createRelationshipInfo(edgeData)) {
    
    companion object Companion {
        /**
         * Create relationship info based on edge data
         */
        private fun createRelationshipInfo(edgeData: GraphvizEdgeData): DiagramRelationshipInfo {
            val builder = DiagramRelationshipInfoAdapter.Builder()
            
            // Set name
            builder.setName(edgeData.label ?: "edge")
            
            // Set line type based on style
            val lineType = when (edgeData.getStyle()) {
                "dashed" -> DiagramLineType.DASHED
                "dotted" -> DiagramLineType.DOTTED
                "bold" -> DiagramLineType.SOLID
                else -> DiagramLineType.SOLID
            }
            builder.setLineType(lineType)
            
            // Set arrows based on edge type and attributes
            when (edgeData.edgeType) {
                GraphvizEdgeType.DIRECTED -> {
                    builder.setSourceArrow(DiagramRelationshipInfo.NONE)
                    
                    val arrowHead = edgeData.getArrowHead()
                    val targetArrow = when (arrowHead) {
                        "diamond" -> DiagramRelationshipInfo.DIAMOND
                        "box" -> DiagramRelationshipInfo.ANGLE
                        "dot" -> DiagramRelationshipInfo.CIRCLE
                        "none" -> DiagramRelationshipInfo.NONE
                        else -> DiagramRelationshipInfo.ANGLE
                    }
                    builder.setTargetArrow(targetArrow)
                }
                GraphvizEdgeType.UNDIRECTED -> {
                    builder.setSourceArrow(DiagramRelationshipInfo.NONE)
                    builder.setTargetArrow(DiagramRelationshipInfo.NONE)
                }
            }
            
            // Set label if present
            edgeData.label?.let { label ->
                builder.setUpperTargetLabel(label)
            }
            
            return builder.create()
        }
        
        // Predefined relationship types for common Graphviz edge styles
        val DIRECTED_EDGE = DiagramRelationshipInfoAdapter.Builder()
            .setName("DIRECTED")
            .setLineType(DiagramLineType.SOLID)
            .setSourceArrow(DiagramRelationshipInfo.NONE)
            .setTargetArrow(DiagramRelationshipInfo.ANGLE)
            .create()
            
        val UNDIRECTED_EDGE = DiagramRelationshipInfoAdapter.Builder()
            .setName("UNDIRECTED")
            .setLineType(DiagramLineType.SOLID)
            .setSourceArrow(DiagramRelationshipInfo.NONE)
            .setTargetArrow(DiagramRelationshipInfo.NONE)
            .create()
            
        val DASHED_EDGE = DiagramRelationshipInfoAdapter.Builder()
            .setName("DASHED")
            .setLineType(DiagramLineType.DASHED)
            .setSourceArrow(DiagramRelationshipInfo.NONE)
            .setTargetArrow(DiagramRelationshipInfo.ANGLE)
            .create()
    }
    
    /**
     * Get the edge data
     */
    fun getEdgeData(): GraphvizEdgeData = edgeData
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphvizEntityEdge) return false
        return edgeData == other.edgeData
    }
    
    override fun hashCode(): Int {
        return edgeData.hashCode()
    }
    
    override fun toString(): String {
        return "GraphvizDiagramEdge(${edgeData})"
    }
}
