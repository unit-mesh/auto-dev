package cc.unitmesh.diagram.graphviz

import cc.unitmesh.diagram.graphviz.parser.DotFileParser
import org.junit.jupiter.api.Test

class SimpleParserTest {
    
    @Test
    fun testBasicParsing() {
        val parser = DotFileParser()
        val content = "digraph G { A -> B; }"
        
        try {
            val result = parser.parse(content)
            println("Nodes: ${result.nodes.size}")
            println("Edges: ${result.edges.size}")
            
            result.nodes.forEach { node ->
                println("Node: ${node.getName()}")
            }
            
            result.edges.forEach { edge ->
                println("Edge: ${edge.sourceNodeId} -> ${edge.targetNodeId}")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
