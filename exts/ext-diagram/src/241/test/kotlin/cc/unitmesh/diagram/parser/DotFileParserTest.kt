package cc.unitmesh.diagram.parser

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

    @Test
    fun `should parse record nodes with fields`() {
        val dotContent = """
            digraph ClassDiagram {
                User [shape=record, label="{User|id:Long|name:String|email:String}"];
                Order [shape=record, label="{Order|id:Long|userId:Long|amount:Double}"];
                User -> Order;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(GraphvizGraphType.DIGRAPH, result.graphType)
        assertEquals(0, result.nodes.size) // Should be entities, not simple nodes
        assertEquals(2, result.entities.size)
        assertEquals(1, result.edges.size)

        val userEntity = result.entities.find { it.getName() == "User" }
        assertNotNull(userEntity)
        assertEquals(3, userEntity!!.getFields().size)

        val fields = userEntity.getFields()
        assertEquals("id", fields[0].name)
        assertEquals("Long", fields[0].type)
        assertEquals("name", fields[1].name)
        assertEquals("String", fields[1].type)
        assertEquals("email", fields[2].name)
        assertEquals("String", fields[2].type)
    }

    @Test
    fun `should parse mixed nodes and entities`() {
        val dotContent = """
            digraph Mixed {
                simpleNode [label="Simple"];
                User [shape=record, label="{User|name:String|age:Int}"];
                simpleNode -> User;
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.nodes.size)
        assertEquals(1, result.entities.size)
        assertEquals(1, result.edges.size)

        val simpleNode = result.nodes.first()
        assertEquals("simpleNode", simpleNode.getName())
        assertEquals(GraphvizNodeType.REGULAR, simpleNode.getNodeType())

        val userEntity = result.entities.first()
        assertEquals("User", userEntity.getName())
        assertEquals(2, userEntity.getFields().size)
    }

    @Test
    fun `should handle empty record labels`() {
        val dotContent = """
            digraph Empty {
                EmptyRecord [shape=record, label="{}"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.nodes.size) // Should fallback to simple node
        assertEquals(0, result.entities.size)
    }

    @Test
    fun `should parse fields with ports`() {
        val dotContent = """
            digraph Ports {
                Node [shape=record, label="{<port1>field1:String|<port2>field2:Int}"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(1, result.entities.size)
        val entity = result.entities.first()
        assertEquals(2, entity.getFields().size)

        val field1 = entity.getFields()[0]
        assertEquals("field1", field1.name)
        assertEquals("String", field1.type)

        val field2 = entity.getFields()[1]
        assertEquals("field2", field2.name)
        assertEquals("Int", field2.type)
    }

    @Test
    fun `should parse complex class diagram with methods`() {
        val dotContent = """
            digraph ClassDiagram {
                User [shape=record, label="{User|id:Long|name:String|+getName():String|+setName(name:String):void}"];
                Order [shape=record, label="{Order|id:Long|amount:Double|+getTotal():Double}"];
                User -> Order [label="has many"];
            }
        """.trimIndent()

        val result = parser.parse(dotContent)

        assertEquals(2, result.entities.size)
        assertEquals(1, result.edges.size)

        val userEntity = result.entities.find { it.getName() == "User" }
        assertNotNull(userEntity)
        assertEquals(5, userEntity!!.getFields().size) // 2 fields + 3 methods

        val fields = userEntity.getFields()
        assertEquals("id", fields[0].name)
        assertEquals("Long", fields[0].type)
        assertEquals("name", fields[1].name)
        assertEquals("String", fields[1].type)
        assertEquals("+getName()", fields[2].name)
        assertEquals("String", fields[2].type)
    }
}
