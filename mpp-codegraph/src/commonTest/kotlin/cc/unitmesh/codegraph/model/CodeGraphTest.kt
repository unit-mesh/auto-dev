package cc.unitmesh.codegraph.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeGraphTest {
    
    @Test
    fun testCodeGraphCreation() {
        val node1 = CodeNode(
            id = "1",
            type = CodeElementType.CLASS,
            name = "MyClass",
            packageName = "com.example",
            filePath = "MyClass.java",
            startLine = 1,
            endLine = 10,
            startColumn = 0,
            endColumn = 0,
            qualifiedName = "com.example.MyClass",
            content = "class MyClass {}"
        )
        
        val node2 = CodeNode(
            id = "2",
            type = CodeElementType.METHOD,
            name = "myMethod",
            packageName = "com.example",
            filePath = "MyClass.java",
            startLine = 5,
            endLine = 8,
            startColumn = 4,
            endColumn = 4,
            qualifiedName = "com.example.MyClass.myMethod",
            content = "void myMethod() {}"
        )
        
        val relationship = CodeRelationship(
            sourceId = "1",
            targetId = "2",
            type = RelationshipType.MADE_OF
        )
        
        val graph = CodeGraph(
            nodes = listOf(node1, node2),
            relationships = listOf(relationship)
        )
        
        assertEquals(2, graph.nodes.size)
        assertEquals(1, graph.relationships.size)
    }
    
    @Test
    fun testGetNodeById() {
        val node = CodeNode(
            id = "test-id",
            type = CodeElementType.CLASS,
            name = "TestClass",
            packageName = "com.test",
            filePath = "Test.java",
            startLine = 1,
            endLine = 10,
            startColumn = 0,
            endColumn = 0,
            qualifiedName = "com.test.TestClass",
            content = "class TestClass {}"
        )
        
        val graph = CodeGraph(nodes = listOf(node), relationships = emptyList())
        
        val found = graph.getNodeById("test-id")
        assertNotNull(found)
        assertEquals("TestClass", found.name)
    }
    
    @Test
    fun testGetNodesByType() {
        val classNode = CodeNode(
            id = "1",
            type = CodeElementType.CLASS,
            name = "MyClass",
            packageName = "com.example",
            filePath = "MyClass.java",
            startLine = 1,
            endLine = 10,
            startColumn = 0,
            endColumn = 0,
            qualifiedName = "com.example.MyClass",
            content = "class MyClass {}"
        )
        
        val methodNode = CodeNode(
            id = "2",
            type = CodeElementType.METHOD,
            name = "myMethod",
            packageName = "com.example",
            filePath = "MyClass.java",
            startLine = 5,
            endLine = 8,
            startColumn = 4,
            endColumn = 4,
            qualifiedName = "com.example.MyClass.myMethod",
            content = "void myMethod() {}"
        )
        
        val graph = CodeGraph(
            nodes = listOf(classNode, methodNode),
            relationships = emptyList()
        )
        
        val classes = graph.getNodesByType(CodeElementType.CLASS)
        assertEquals(1, classes.size)
        assertEquals("MyClass", classes[0].name)
        
        val methods = graph.getNodesByType(CodeElementType.METHOD)
        assertEquals(1, methods.size)
        assertEquals("myMethod", methods[0].name)
    }
    
    @Test
    fun testGetRelationships() {
        val rel1 = CodeRelationship(
            sourceId = "1",
            targetId = "2",
            type = RelationshipType.MADE_OF
        )
        
        val rel2 = CodeRelationship(
            sourceId = "1",
            targetId = "3",
            type = RelationshipType.DEPENDS_ON
        )
        
        val graph = CodeGraph(
            nodes = emptyList(),
            relationships = listOf(rel1, rel2)
        )
        
        val outgoing = graph.getOutgoingRelationships("1")
        assertEquals(2, outgoing.size)
        
        val incoming = graph.getIncomingRelationships("2")
        assertEquals(1, incoming.size)
        
        val madeOf = graph.getRelationshipsByType(RelationshipType.MADE_OF)
        assertEquals(1, madeOf.size)
    }
}

