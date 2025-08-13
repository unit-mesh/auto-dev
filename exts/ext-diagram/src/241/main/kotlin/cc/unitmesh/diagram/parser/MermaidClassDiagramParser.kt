package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.model.*
import cc.unitmesh.diagram.parser.mermaid.*

/**
 * Parser for Mermaid class diagrams
 * Converts Mermaid classDiagram syntax to internal GraphvizDiagramData model
 *
 * This parser is based on the official Mermaid classDiagram.jison grammar
 * and provides comprehensive support for Mermaid class diagram syntax.
 */
class MermaidClassDiagramParser {
    
    /**
     * Parse Mermaid class diagram content from string
     */
    fun parse(mermaidContent: String): GraphvizDiagramData {
        return try {
            // Use the new lexer and parser
            val lexer = MermaidLexer(mermaidContent)
            val tokens = lexer.tokenize()

            val parser = MermaidParser(tokens)
            val parseResult = parser.parse()

            when (parseResult) {
                is ParseResult.Success -> convertAstToGraphvizData(parseResult.ast)
                is ParseResult.Error -> {
                    // Log errors and return empty data
                    parseResult.errors.forEach { error ->
                        println("Parse error: ${error.message} at ${error.position}")
                    }
                    createEmptyData()
                }
            }
        } catch (e: Exception) {
            println("Exception during parsing: ${e.message}")
            e.printStackTrace()
            createEmptyData()
        }
    }

    /**
     * Parse Mermaid class diagram from AST
     */
    fun parse(ast: ClassDiagramNode): GraphvizDiagramData {
        return convertAstToGraphvizData(ast)
    }

    /**
     * Convert AST to GraphvizDiagramData
     */
    private fun convertAstToGraphvizData(ast: ClassDiagramNode): GraphvizDiagramData {
        val entities = mutableMapOf<String, MutableList<GraphvizNodeField>>()
        val edges = mutableListOf<GraphvizEdgeData>()
        val graphAttributes = mutableMapOf<String, String>()

        // Process all statements
        for (statement in ast.statements) {
            when (statement) {
                is ClassStatementNode -> {
                    val fields = statement.members.map { member ->
                        convertMemberToField(member)
                    }.toMutableList()

                    // Merge with existing fields if entity already exists
                    if (entities.containsKey(statement.className)) {
                        entities[statement.className]!!.addAll(fields)
                    } else {
                        entities[statement.className] = fields
                    }
                }

                is RelationStatementNode -> {
                    val edge = convertRelationToEdge(statement)
                    edges.add(edge)

                    // Ensure both source and target classes exist as entities
                    if (!entities.containsKey(statement.sourceClass)) {
                        entities[statement.sourceClass] = mutableListOf()
                    }
                    if (!entities.containsKey(statement.targetClass)) {
                        entities[statement.targetClass] = mutableListOf()
                    }
                }

                is MemberStatementNode -> {
                    // Add member to the class entity
                    val field = convertMemberToField(statement.member)
                    if (entities.containsKey(statement.className)) {
                        entities[statement.className]!!.add(field)
                    } else {
                        entities[statement.className] = mutableListOf(field)
                    }
                }

                is ClassAnnotationStatementNode -> {
                    // Handle class annotation (ClassName : ChangeType)
                    // Update the change status of the class if it exists
                    if (entities.containsKey(statement.className)) {
                        // The class already exists, we can update its change status
                        // For now, we'll store this information in graph attributes
                        graphAttributes["${statement.className}_change"] = statement.annotation
                    } else {
                        // Create an empty class with change status
                        entities[statement.className] = mutableListOf()
                        graphAttributes["${statement.className}_change"] = statement.annotation
                    }
                }

                is DirectionStatementNode -> {
                    graphAttributes["direction"] = statement.direction.name
                }

                is AccessibilityStatementNode -> {
                    when (statement.type) {
                        AccessibilityType.TITLE -> graphAttributes["title"] = statement.value
                        AccessibilityType.DESCRIPTION -> graphAttributes["description"] = statement.value
                        AccessibilityType.DESCRIPTION_MULTILINE -> graphAttributes["description"] = statement.value
                    }
                }

                // Handle other statement types as needed
                else -> {
                    // For now, ignore other statement types
                }
            }
        }

        graphAttributes["type"] = "mermaid_class_diagram"

        // Convert entities map to list
        val entityList = entities.map { (className, fields) ->
            GraphvizEntityNodeData(className, fields)
        }

        return GraphvizDiagramData(
            nodes = emptyList(),
            entities = entityList,
            edges = edges,
            graphAttributes = graphAttributes,
            graphType = GraphvizGraphType.DIGRAPH
        )
    }
    
    /**
     * Convert AST member node to GraphvizNodeField
     */
    private fun convertMemberToField(member: MemberNode): GraphvizNodeField {
        val visibilitySymbol = when (member.visibility) {
            VisibilityType.PUBLIC -> "+"
            VisibilityType.PRIVATE -> "-"
            VisibilityType.PROTECTED -> "#"
            VisibilityType.PACKAGE -> "~"
        }

        // Convert change status from AST to GraphvizNodeField ChangeStatus
        val changeStatus = when (member.changeStatus) {
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.ADDED -> cc.unitmesh.diagram.model.ChangeStatus.ADDED
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.REMOVED -> cc.unitmesh.diagram.model.ChangeStatus.REMOVED
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.MODIFIED -> cc.unitmesh.diagram.model.ChangeStatus.UNCHANGED
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.UNCHANGED -> cc.unitmesh.diagram.model.ChangeStatus.UNCHANGED
        }

        val name = if (member.isMethod) {
            "${member.name}()"
        } else {
            member.name
        }

        // Include change status prefix in the name if it's not unchanged
        val nameWithChangeStatus = when (member.changeStatus) {
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.ADDED -> "+$name"
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.REMOVED -> "-$name"
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.MODIFIED -> "~$name"
            cc.unitmesh.diagram.parser.mermaid.ChangeStatus.UNCHANGED -> "$visibilitySymbol$name"
        }

        return GraphvizNodeField(
            name = nameWithChangeStatus,
            type = member.type ?: if (member.isMethod) "method" else "field",
            required = false,
            changeStatus = changeStatus,
            isMethodField = member.isMethod
        )
    }

    /**
     * Convert AST relation node to GraphvizEdgeData
     */
    private fun convertRelationToEdge(relation: RelationStatementNode): GraphvizEdgeData {
        val attributes = mutableMapOf<String, String>()

        // Determine arrow style based on relation types
        when {
            relation.relation.type1 == RelationType.EXTENSION || relation.relation.type2 == RelationType.EXTENSION -> {
                attributes["arrowhead"] = "empty"
            }
            relation.relation.type1 == RelationType.COMPOSITION || relation.relation.type2 == RelationType.COMPOSITION -> {
                attributes["arrowhead"] = "diamond"
                attributes["style"] = "filled"
            }
            relation.relation.type1 == RelationType.AGGREGATION || relation.relation.type2 == RelationType.AGGREGATION -> {
                attributes["arrowhead"] = "diamond"
            }
            relation.relation.type1 == RelationType.DEPENDENCY || relation.relation.type2 == RelationType.DEPENDENCY -> {
                attributes["style"] = "dashed"
            }
        }

        // Handle line type
        if (relation.relation.lineType == LineType.DOTTED_LINE) {
            attributes["style"] = "dotted"
        }

        val label = relation.relationLabel ?: when {
            relation.relation.type1 == RelationType.EXTENSION || relation.relation.type2 == RelationType.EXTENSION -> "extends"
            relation.relation.type1 == RelationType.COMPOSITION || relation.relation.type2 == RelationType.COMPOSITION -> "composed of"
            relation.relation.type1 == RelationType.AGGREGATION || relation.relation.type2 == RelationType.AGGREGATION -> "aggregates"
            relation.relation.type1 == RelationType.DEPENDENCY || relation.relation.type2 == RelationType.DEPENDENCY -> "depends on"
            else -> null
        }

        // For inheritance relationships, reverse the direction
        // In Mermaid: "Animal <|-- Duck" means Duck extends Animal
        // So the edge should go from Duck to Animal
        val (actualSource, actualTarget) = if (relation.relation.type1 == RelationType.EXTENSION || relation.relation.type2 == RelationType.EXTENSION) {
            // For inheritance, reverse the direction
            relation.targetClass to relation.sourceClass
        } else {
            // For other relationships, keep the original direction
            relation.sourceClass to relation.targetClass
        }

        return GraphvizEdgeData(
            sourceNodeId = actualSource,
            targetNodeId = actualTarget,
            label = label,
            attributes = attributes,
            edgeType = GraphvizEdgeType.DIRECTED
        )
    }

    private fun createEmptyData(): GraphvizDiagramData {
        return GraphvizDiagramData(
            nodes = emptyList(),
            entities = emptyList(),
            edges = emptyList(),
            graphAttributes = emptyMap(),
            graphType = GraphvizGraphType.DIGRAPH
        )
    }

}
