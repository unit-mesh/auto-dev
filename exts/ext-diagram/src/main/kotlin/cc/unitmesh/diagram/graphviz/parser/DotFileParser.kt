package cc.unitmesh.diagram.graphviz.parser

import cc.unitmesh.diagram.graphviz.model.*
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser

/**
 * Parser for DOT files using guru.nidi.graphviz library
 * Converts DOT format to internal GraphvizDiagramData model
 */
class DotFileParser {
    
    /**
     * Parse DOT content from string
     */
    fun parse(dotContent: String): GraphvizDiagramData {
        return try {
            // Use string directly for parsing
            val graph = Parser().read(dotContent)
            convertToGraphvizData(graph)
        } catch (e: Exception) {
            // Return empty data if parsing fails
            GraphvizDiagramData(
                nodes = emptyList(),
                edges = emptyList(),
                graphAttributes = emptyMap(),
                graphType = GraphvizGraphType.DIGRAPH
            )
        }
    }
    

    
    /**
     * Convert guru.nidi.graphviz MutableGraph to our internal model
     */
    private fun convertToGraphvizData(graph: MutableGraph): GraphvizDiagramData {
        val nodes = mutableListOf<GraphvizSimpleNodeData>()
        val edges = mutableListOf<GraphvizEdgeData>()
        
        // Extract graph type
        val graphType = if (graph.isDirected) {
            GraphvizGraphType.DIGRAPH
        } else {
            GraphvizGraphType.GRAPH
        }
        
        // Extract graph attributes
        val graphAttributes = try {
            graph.graphAttrs().associate { attr ->
                attr.key to attr.value.toString()
            }
        } catch (e: Exception) {
            emptyMap()
        }
        
        // Extract nodes
        graph.nodes().forEach { node ->
            val nodeId = node.name().toString()
            val nodeAttrs = try {
                node.attrs().associate { attr ->
                    attr.key to attr.value.toString()
                }
            } catch (e: Exception) {
                emptyMap()
            }
            
            // Extract label from attributes or use node ID
            val label = nodeAttrs["label"] ?: nodeId
            
            // Determine node type based on shape
            val nodeType = when (nodeAttrs["shape"]) {
                "record", "Mrecord" -> GraphvizNodeType.RECORD
                "box", "rectangle" -> GraphvizNodeType.REGULAR
                else -> GraphvizNodeType.REGULAR
            }
            
            nodes.add(
                GraphvizSimpleNodeData(
                    id = nodeId,
                    label = if (label != nodeId) label else null,
                    attributes = nodeAttrs,
                    nodeType = nodeType
                )
            )
        }
        
        // Extract edges
        graph.edges().forEach { edge ->
            val sourceId = edge.from()?.name()?.toString() ?: "unknown"
            val targetId = edge.to()?.name()?.toString() ?: "unknown"

            val edgeAttrs = try {
                edge.attrs().associate { attr ->
                    attr.key to attr.value.toString()
                }
            } catch (e: Exception) {
                emptyMap()
            }
            
            val label = edgeAttrs["label"]
            
            val edgeType = if (graph.isDirected) {
                GraphvizEdgeType.DIRECTED
            } else {
                GraphvizEdgeType.UNDIRECTED
            }
            
            edges.add(
                GraphvizEdgeData(
                    sourceNodeId = sourceId,
                    targetNodeId = targetId,
                    label = label,
                    attributes = edgeAttrs,
                    edgeType = edgeType
                )
            )
        }
        
        return GraphvizDiagramData(
            nodes = nodes,
            edges = edges,
            graphAttributes = graphAttributes,
            graphType = graphType
        )
    }
}
