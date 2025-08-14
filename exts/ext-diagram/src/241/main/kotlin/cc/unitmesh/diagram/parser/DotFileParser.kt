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

    fun parse(content: String): GraphvizDiagramData {
        return try {
            if (isMermaidClassDiagram(content)) {
                mermaidParser.parse(content)
            } else {
                val graph = Parser().read(content)
                convertToGraphvizData(graph)
            }
        } catch (e: Exception) {
            GraphvizDiagramData(
                nodes = emptyList(),
                entities = emptyList(),
                edges = emptyList(),
                graphAttributes = emptyMap(),
                graphType = GraphvizGraphType.DIGRAPH
            )
        }
    }


    private fun isMermaidClassDiagram(content: String): Boolean {
        return content.contains("classDiagram")
    }

    private fun convertToGraphvizData(graph: MutableGraph): GraphvizDiagramData {
        val nodes = mutableListOf<GraphvizSimpleNodeData>()
        val entities = mutableListOf<GraphvizEntityNodeData>()
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

            // Parse fields for record-type nodes
            if (nodeType == GraphvizNodeType.RECORD) {
                val fields = parseRecordFields(label)
                if (fields.isNotEmpty()) {
                    entities.add(
                        GraphvizEntityNodeData(
                            name = nodeId,
                            fields = fields
                        )
                    )
                } else {
                    // Fallback to simple node if no fields found
                    nodes.add(
                        GraphvizSimpleNodeData(
                            id = nodeId,
                            label = if (label != nodeId) label else null,
                            attributes = nodeAttrs,
                            nodeType = nodeType
                        )
                    )
                }
            } else {
                nodes.add(
                    GraphvizSimpleNodeData(
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
            entities = entities,
            edges = edges,
            graphAttributes = graphAttributes,
            graphType = graphType
        )
    }

    /**
     * Parse fields from a Graphviz record label
     * Record format examples:
     * - "{field1|field2|field3}"
     * - "{field1:type1|field2:type2}"
     * - "{<port1>field1|<port2>field2:type2}"
     */
    private fun parseRecordFields(label: String): List<GraphvizNodeField> {
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
    private fun parseRecordField(fieldPart: String): GraphvizNodeField? {
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
            GraphvizNodeField(
                name = fieldName,
                type = fieldType,
                required = false // Could be enhanced to detect required fields
            )
        } else {
            null
        }
    }
}
