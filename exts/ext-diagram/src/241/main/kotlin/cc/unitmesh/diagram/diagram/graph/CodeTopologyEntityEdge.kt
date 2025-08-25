package cc.unitmesh.diagram.diagram.graph

import com.intellij.diagram.DiagramEdgeBase
import com.intellij.diagram.DiagramNode
import com.intellij.diagram.DiagramRelationshipInfo
import com.intellij.diagram.DiagramRelationshipInfoAdapter
import com.intellij.diagram.presentation.DiagramLineType
import cc.unitmesh.diagram.model.GraphNodeData
import cc.unitmesh.diagram.model.GraphEdgeData
import cc.unitmesh.diagram.model.GraphvizEdgeType

/**
 * Diagram edge implementation for Graphviz graphs
 * Similar to JdlDiagramEntityEdge in JHipster UML implementation
 */
class CodeTopologyEntityEdge(
    source: DiagramNode<GraphNodeData>,
    target: DiagramNode<GraphNodeData>,
    private val edgeData: GraphEdgeData
) : DiagramEdgeBase<GraphNodeData>(source, target, createRelationshipInfo(edgeData)) {
    
    companion object Companion {
        /**
         * Create relationship info based on edge data
         */
        private fun createRelationshipInfo(edgeData: GraphEdgeData): DiagramRelationshipInfo {
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
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeTopologyEntityEdge) return false
        return edgeData == other.edgeData
    }
    
    override fun hashCode(): Int {
        return edgeData.hashCode()
    }
    
    override fun toString(): String {
        return "GraphvizDiagramEdge(${edgeData})"
    }
}
