package cc.unitmesh.diagram.graphviz

import cc.unitmesh.diagram.graphviz.parser.DotFileParser
import org.junit.jupiter.api.Test

class DebugParserTest {
    
    @Test
    fun testSimpleParser() {
        val parser = DotFileParser()
        val content = """
            digraph G {
                A -> B;
                B -> C;
            }
        """.trimIndent()
        
        println("Testing DOT content:")
        println(content)
        println("=".repeat(50))
        
        val result = parser.parse(content)
        
        println("Parse result:")
        println("Nodes: ${result.nodes.size}")
        result.nodes.forEach { node ->
            println("  - ${node.getName()}: ${node.javaClass.simpleName}")
        }
        
        println("Edges: ${result.edges.size}")
        result.edges.forEach { edge ->
            println("  - ${edge.sourceNodeId} -> ${edge.targetNodeId}")
        }
        
        println("Graph type: ${result.graphType}")
        println("Graph attributes: ${result.graphAttributes}")
    }
}
