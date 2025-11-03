package cc.unitmesh.codegraph.parser.jvm

import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmCodeParserTest {
    
    private val parser = JvmCodeParser()
    
    @Test
    fun testParseSimpleJavaClass() = runTest {
        val sourceCode = """
            package com.example;
            
            public class HelloWorld {
                private String message;
                
                public void sayHello() {
                    System.out.println("Hello");
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "HelloWorld.java", Language.JAVA)
        
        assertTrue(nodes.isNotEmpty())
        
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("HelloWorld", classNode.name)
        assertEquals("com.example", classNode.packageName)
    }
    
    @Test
    fun testParseJavaInterface() = runTest {
        val sourceCode = """
            package com.example;
            
            public interface MyInterface {
                void doSomething();
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "MyInterface.java", Language.JAVA)
        
        val interfaceNode = nodes.find { it.type == CodeElementType.INTERFACE }
        assertTrue(interfaceNode != null)
        assertEquals("MyInterface", interfaceNode.name)
    }
    
    @Test
    fun testParseCodeGraph() = runTest {
        val files = mapOf(
            "HelloWorld.java" to """
                package com.example;
                
                public class HelloWorld {
                    public void sayHello() {
                        System.out.println("Hello");
                    }
                }
            """.trimIndent(),
            "Greeter.java" to """
                package com.example;
                
                public class Greeter {
                    public void greet() {
                        System.out.println("Greetings");
                    }
                }
            """.trimIndent()
        )
        
        val graph = parser.parseCodeGraph(files, Language.JAVA)

        assertTrue(graph.nodes.isNotEmpty())
        assertEquals("2", graph.metadata["fileCount"])
        assertEquals("JAVA", graph.metadata["language"])
        
        val classes = graph.getNodesByType(CodeElementType.CLASS)
        assertTrue(classes.size >= 2)
    }
}

