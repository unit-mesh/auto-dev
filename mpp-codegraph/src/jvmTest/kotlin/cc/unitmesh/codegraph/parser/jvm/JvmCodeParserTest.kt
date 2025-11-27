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
    fun testParseJavaConstructor() = runTest {
        val sourceCode = """
            package com.example;
            
            public class UserService {
                private String name;
                
                public UserService(String name) {
                    this.name = name;
                }
                
                public void doSomething() {
                    System.out.println(name);
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "UserService.java", Language.JAVA)
        
        assertTrue(nodes.isNotEmpty())
        
        // Should find class
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("UserService", classNode.name)
        
        // Should find constructor
        val constructorNode = nodes.find { it.type == CodeElementType.CONSTRUCTOR }
        assertTrue(constructorNode != null, "Constructor should be detected")
        assertEquals("<init>", constructorNode.name)
        assertEquals("UserService", constructorNode.metadata["parent"])
        
        // Should find method
        val methodNode = nodes.find { it.type == CodeElementType.METHOD }
        assertTrue(methodNode != null)
        assertEquals("doSomething", methodNode.name)
    }
    
    @Test
    fun testParseKotlinConstructor() = runTest {
        val sourceCode = """
            package com.example
            
            class UserService(private val name: String) {
                fun doSomething() {
                    println(name)
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "UserService.kt", Language.KOTLIN)
        
        assertTrue(nodes.isNotEmpty())
        
        // Should find class
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("UserService", classNode.name)
        
        // Should find primary constructor
        val constructorNode = nodes.find { it.type == CodeElementType.CONSTRUCTOR }
        assertTrue(constructorNode != null, "Kotlin primary constructor should be detected")
        assertEquals("<init>", constructorNode.name)
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

