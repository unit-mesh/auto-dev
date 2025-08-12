package cc.unitmesh.diagram.graphviz.parser

import cc.unitmesh.diagram.graphviz.model.GraphvizEdgeType
import cc.unitmesh.diagram.graphviz.model.GraphvizGraphType
import cc.unitmesh.diagram.graphviz.model.GraphvizNodeType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DotFileParserTest {
    
    private val parser = DotFileParser()
    
    @Test
    fun `should parse simple directed graph`() {
        val dotContent = """
            digraph G {
                A -> B;
                B -> C;
                A -> C;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphvizGraphType.DIGRAPH, result.graphType)
        assertEquals(3, result.nodes.size)
        assertEquals(3, result.edges.size)
        
        // Check nodes
        val nodeNames = result.nodes.map { it.getName() }.toSet()
        assertTrue(nodeNames.contains("A"))
        assertTrue(nodeNames.contains("B"))
        assertTrue(nodeNames.contains("C"))
        
        // Check edges
        val edges = result.edges.toList()
        assertTrue(edges.any { it.sourceNodeId == "A" && it.targetNodeId == "B" })
        assertTrue(edges.any { it.sourceNodeId == "B" && it.targetNodeId == "C" })
        assertTrue(edges.any { it.sourceNodeId == "A" && it.targetNodeId == "C" })
        
        // All edges should be directed
        assertTrue(edges.all { it.edgeType == GraphvizEdgeType.DIRECTED })
    }
    
    @Test
    fun `should parse graph with node attributes`() {
        val dotContent = """
            digraph G {
                A [label="Node A", shape=box, color=red];
                B [label="Node B", shape=circle];
                A -> B;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)
        
        val nodeA = result.getNodeById("A")
        assertNotNull(nodeA)
        assertEquals("Node A", nodeA?.getAttribute("label"))
        assertEquals("box", nodeA?.getAttribute("shape"))
        assertEquals("red", nodeA?.getAttribute("color"))
        
        val nodeB = result.getNodeById("B")
        assertNotNull(nodeB)
        assertEquals("Node B", nodeB?.getAttribute("label"))
        assertEquals("circle", nodeB?.getAttribute("shape"))
    }
    
    @Test
    fun `should parse graph with edge attributes`() {
        val dotContent = """
            digraph G {
                A -> B [label="edge label", color=blue, style=dashed];
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(2, result.nodes.size)
        assertEquals(1, result.edges.size)
        
        val edge = result.edges.first()
        assertEquals("A", edge.sourceNodeId)
        assertEquals("B", edge.targetNodeId)
        assertEquals("edge label", edge.label)
        assertEquals("blue", edge.getColor())
        assertEquals("dashed", edge.getStyle())
    }
    
    @Test
    fun `should handle undirected graph`() {
        val dotContent = """
            graph G {
                A -- B;
                B -- C;
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphvizGraphType.GRAPH, result.graphType)
        assertEquals(3, result.nodes.size)
        assertEquals(2, result.edges.size)
        
        // All edges should be undirected
        assertTrue(result.edges.all { it.edgeType == GraphvizEdgeType.UNDIRECTED })
    }
    
    @Test
    fun `should handle empty graph`() {
        val dotContent = """
            digraph G {
            }
        """.trimIndent()
        
        val result = parser.parse(dotContent)
        
        assertEquals(GraphvizGraphType.DIGRAPH, result.graphType)
        assertEquals(0, result.nodes.size)
        assertEquals(0, result.edges.size)
    }
    
    @Test
    fun `should handle invalid DOT content gracefully`() {
        val invalidContent = "this is not valid DOT content"
        
        val result = parser.parse(invalidContent)
        
        // Should return empty data instead of throwing exception
        assertEquals(0, result.nodes.size)
        assertEquals(0, result.edges.size)
    }
}
