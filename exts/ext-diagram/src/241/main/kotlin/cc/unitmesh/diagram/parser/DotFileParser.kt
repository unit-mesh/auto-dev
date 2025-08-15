package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.model.*
import guru.nidi.graphviz.model.MutableGraph
import guru.nidi.graphviz.parse.Parser

/**
 * Parser for DOT files using guru.nidi.graphviz library
 * Converts DOT format to internal GraphvizDiagramData model
 * Also supports Mermaid class diagrams
 */
class DotFileParser {
    private val mermaidParser = MermaidClassDiagramParser()

    fun parse(content: String): GraphDiagramData {
        return try {
            if (isMermaidClassDiagram(content)) {
                mermaidParser.parse(content)
            } else {
                val graph = Parser().read(content)
                convertToGraphvizData(graph)
            }
        } catch (e: Exception) {
            GraphDiagramData(
                nodes = emptyList(),
                entities = emptyList(),
                edges = emptyList(),
                subgraphs = emptyList(),
                graphAttributes = emptyMap(),
                graphType = GraphGraphType.DIGRAPH
            )
        }
    }


    private fun isMermaidClassDiagram(content: String): Boolean {
        return content.contains("classDiagram")
    }

    private fun convertToGraphvizData(graph: MutableGraph): GraphDiagramData {
        val nodes = mutableListOf<GraphSimpleNodeData>()
        val entities = mutableListOf<GraphEntityNodeData>()
        val edges = mutableListOf<GraphEdgeData>()
        val subgraphs = mutableListOf<GraphSubgraphData>()

        // Extract graph type
        val graphType = if (graph.isDirected) {
            GraphGraphType.DIGRAPH
        } else {
            GraphGraphType.GRAPH
        }

        // Extract graph attributes
        val graphAttributes = try {
            graph.graphAttrs().associate { attr ->
                attr.key to attr.value.toString()
            }
        } catch (e: Exception) {
            emptyMap()
        }

        // Extract subgraphs first
        extractSubgraphs(graph, subgraphs)
        
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
            val rawLabel = nodeAttrs["label"] ?: nodeId
            val label = if (HtmlLabelParser.isHtmlLabel(rawLabel)) {
                HtmlLabelParser.parseHtmlLabel(rawLabel)
            } else {
                rawLabel
            }
            
            // Determine node type based on shape
            val nodeType = when (nodeAttrs["shape"]) {
                "record", "Mrecord" -> GraphvizNodeType.RECORD
                "box", "rectangle" -> GraphvizNodeType.REGULAR
                else -> GraphvizNodeType.REGULAR
            }

            // Parse fields for record-type nodes
            if (nodeType == GraphvizNodeType.RECORD) {
                val fields = parseRecordFields(label)
                if (fields.isNotEmpty()) {
                    entities.add(
                        GraphEntityNodeData(
                            name = nodeId,
                            fields = fields
                        )
                    )
                } else {
                    // Fallback to simple node if no fields found
                    nodes.add(
                        GraphSimpleNodeData(
                            id = nodeId,
                            label = if (label != nodeId) label else null,
                            attributes = nodeAttrs,
                            nodeType = nodeType
                        )
                    )
                }
            } else {
                nodes.add(
                    GraphSimpleNodeData(
                        id = nodeId,
                        label = if (label != nodeId) label else null,
                        attributes = nodeAttrs,
                        nodeType = nodeType
                    )
                )
            }
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
                GraphEdgeData(
                    sourceNodeId = sourceId,
                    targetNodeId = targetId,
                    label = label,
                    attributes = edgeAttrs,
                    edgeType = edgeType
                )
            )
        }
        
        return GraphDiagramData(
            nodes = nodes,
            entities = entities,
            edges = edges,
            subgraphs = subgraphs,
            graphAttributes = graphAttributes,
            graphType = graphType
        )
    }

    /**
     * Extract subgraphs from the main graph
     */
    private fun extractSubgraphs(graph: MutableGraph, subgraphs: MutableList<GraphSubgraphData>) {
        try {
            graph.graphs().forEach { subgraph ->
                val subgraphName = subgraph.name()?.toString() ?: "unnamed_subgraph"
                val isCluster = subgraphName.startsWith("cluster_")

                // Extract subgraph attributes
                val subgraphAttrs = try {
                    subgraph.graphAttrs().associate { attr ->
                        attr.key to attr.value.toString()
                    }
                } catch (e: Exception) {
                    emptyMap()
                }

                // Extract label from subgraph attributes
                val rawLabel = subgraphAttrs["label"]
                val label = if (rawLabel != null && HtmlLabelParser.isHtmlLabel(rawLabel)) {
                    HtmlLabelParser.parseHtmlLabel(rawLabel)
                } else {
                    rawLabel
                }

                // Extract nodes in this subgraph
                val subgraphNodes = subgraph.nodes().map { it.name().toString() }

                // Extract edges in this subgraph
                val subgraphEdges = mutableListOf<GraphEdgeData>()
                subgraph.edges().forEach { edge ->
                    val sourceId = edge.from()?.name()?.toString() ?: "unknown"
                    val targetId = edge.to()?.name()?.toString() ?: "unknown"

                    val edgeAttrs = try {
                        edge.attrs().associate { attr ->
                            attr.key to attr.value.toString()
                        }
                    } catch (e: Exception) {
                        emptyMap()
                    }

                    val edgeLabel = edgeAttrs["label"]

                    val edgeType = if (graph.isDirected) {
                        GraphvizEdgeType.DIRECTED
                    } else {
                        GraphvizEdgeType.UNDIRECTED
                    }

                    subgraphEdges.add(
                        GraphEdgeData(
                            sourceNodeId = sourceId,
                            targetNodeId = targetId,
                            label = edgeLabel,
                            attributes = edgeAttrs,
                            edgeType = edgeType
                        )
                    )
                }

                subgraphs.add(
                    GraphSubgraphData(
                        name = subgraphName,
                        label = label,
                        nodes = subgraphNodes,
                        edges = subgraphEdges,
                        attributes = subgraphAttrs,
                        isCluster = isCluster
                    )
                )

                // Recursively extract nested subgraphs
                extractSubgraphs(subgraph, subgraphs)
            }
        } catch (e: Exception) {
            // Handle any errors gracefully
            println("Error extracting subgraphs: ${e.message}")
        }
    }

    /**
     * Parse fields from a Graphviz record label
     * Record format examples:
     * - "{field1|field2|field3}"
     * - "{field1:type1|field2:type2}"
     * - "{<port1>field1|<port2>field2:type2}"
     */
    private fun parseRecordFields(label: String): List<GraphNodeField> {
        if (label.isBlank()) return emptyList()

        // Remove outer braces if present
        val cleanLabel = label.trim().removeSurrounding("{", "}")

        if (cleanLabel.isBlank()) return emptyList()

        // Split by | to get individual fields
        val fieldParts = cleanLabel.split("|")

        return fieldParts.mapNotNull { fieldPart ->
            parseRecordField(fieldPart.trim())
        }
    }

    /**
     * Parse a single field from record format
     * Examples:
     * - "fieldName" -> GraphvizNodeField("fieldName", null, false)
     * - "fieldName:String" -> GraphvizNodeField("fieldName", "String", false)
     * - "<port>fieldName:String" -> GraphvizNodeField("fieldName", "String", false)
     */
    private fun parseRecordField(fieldPart: String): GraphNodeField? {
        if (fieldPart.isBlank()) return null

        var cleanField = fieldPart

        // Remove port specification if present (e.g., "<port1>fieldName" -> "fieldName")
        if (cleanField.contains(">")) {
            val portEndIndex = cleanField.indexOf(">")
            if (portEndIndex < cleanField.length - 1) {
                cleanField = cleanField.substring(portEndIndex + 1)
            }
        }

        // Split by : to separate name and type
        val parts = cleanField.split(":", limit = 2)
        val fieldName = parts[0].trim()
        val fieldType = if (parts.size > 1) parts[1].trim().takeIf { it.isNotBlank() } else null

        return if (fieldName.isNotBlank()) {
            GraphNodeField(
                name = fieldName,
                type = fieldType,
                required = false // Could be enhanced to detect required fields
            )
        } else {
            null
        }
    }
}
