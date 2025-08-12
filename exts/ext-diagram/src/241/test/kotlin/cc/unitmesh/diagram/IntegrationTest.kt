package cc.unitmesh.diagram

import cc.unitmesh.diagram.graphviz.parser.DotFileParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class IntegrationTest {
    
    @Test
    fun testCompleteFlow() {
        val parser = DotFileParser()
        val dotContent = """
            digraph G {
                A [label="Node A", shape=box];
                B [label="Node B", shape=ellipse];
                C [label="Node C"];
                
                A -> B [label="edge 1"];
                B -> C [label="edge 2"];
                A -> C [label="direct"];
            }
        """.trimIndent()
        
        println("Testing DOT content:")
        println(dotContent)
        println("=".repeat(50))
        
        val result = parser.parse(dotContent)
        
        println("Parse result:")
        println("Graph type: ${result.graphType}")
        println("Graph attributes: ${result.graphAttributes}")
        
        println("\nNodes (${result.nodes.size}):")
        result.nodes.forEach { node ->
            println("  - ID: ${node.getName()}")
            if (node is cc.unitmesh.diagram.graphviz.model.GraphvizSimpleNodeData) {
                println("    Label: ${node.getDisplayLabel()}")
                println("    Shape: ${node.getShape()}")
                println("    Attributes: ${node.getAttributes()}")
            }
        }
        
        println("\nEdges (${result.edges.size}):")
        result.edges.forEach { edge ->
            println("  - ${edge.sourceNodeId} -> ${edge.targetNodeId}")
            println("    Label: ${edge.label}")
            println("    Type: ${edge.edgeType}")
            println("    Attributes: ${edge.attributes}")
        }
        
        // Verify the results
        assertEquals(3, result.nodes.size, "Should have 3 nodes")
        assertEquals(3, result.edges.size, "Should have 3 edges")
        
        // Check specific nodes
        val nodeNames = result.nodes.map { it.getName() }.toSet()
        assertTrue(nodeNames.contains("A"), "Should contain node A")
        assertTrue(nodeNames.contains("B"), "Should contain node B")
        assertTrue(nodeNames.contains("C"), "Should contain node C")
        
        // Check specific edges
        val edgeConnections = result.edges.map { "${it.sourceNodeId}->${it.targetNodeId}" }.toSet()
        assertTrue(edgeConnections.contains("A->B"), "Should contain edge A->B")
        assertTrue(edgeConnections.contains("B->C"), "Should contain edge B->C")
        assertTrue(edgeConnections.contains("A->C"), "Should contain edge A->C")
    }
}
