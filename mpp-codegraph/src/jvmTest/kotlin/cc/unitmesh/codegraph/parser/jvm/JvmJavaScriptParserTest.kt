package cc.unitmesh.codegraph.parser.jvm

import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmJavaScriptParserTest {
    
    private val parser = JvmCodeParser()
    
    @Test
    fun `should parse JavaScript class`() = runTest {
        val sourceCode = """
            class Calculator {
                constructor() {
                    this.result = 0;
                }
                
                add(a, b) {
                    return a + b;
                }
                
                subtract(a, b) {
                    return a - b;
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "Calculator.js", Language.JAVASCRIPT)
        
        assertTrue(nodes.isNotEmpty())
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("Calculator", classNode.name)
        
        val methods = nodes.filter { it.type == CodeElementType.METHOD }
        assertTrue(methods.size >= 2) // add and subtract
    }
    
    @Test
    fun `should parse JavaScript function`() = runTest {
        val sourceCode = """
            function greet(name) {
                return "Hello, " + name;
            }
            
            const add = (a, b) => a + b;
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "utils.js", Language.JAVASCRIPT)
        
        assertTrue(nodes.isNotEmpty())
        val functions = nodes.filter { it.type == CodeElementType.METHOD }
        assertTrue(functions.isNotEmpty())
    }
    
    @Test
    fun `should parse TypeScript class`() = runTest {
        val sourceCode = """
            class Person {
                name: string;
                age: number;
                
                constructor(name: string, age: number) {
                    this.name = name;
                    this.age = age;
                }
                
                greet(): string {
                    return `Hello, I'm ${'$'}{this.name}`;
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "Person.ts", Language.TYPESCRIPT)
        
        assertTrue(nodes.isNotEmpty())
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("Person", classNode.name)
    }
    
    @Test
    fun `should parse JavaScript code graph`() = runTest {
        val files = mapOf(
            "Calculator.js" to """
                class Calculator {
                    add(a, b) {
                        return a + b;
                    }
                }
            """.trimIndent(),
            "utils.js" to """
                function multiply(a, b) {
                    return a * b;
                }
            """.trimIndent()
        )
        
        val graph = parser.parseCodeGraph(files, Language.JAVASCRIPT)
        
        assertTrue(graph.nodes.isNotEmpty())
        assertEquals("2", graph.metadata["fileCount"])
        assertEquals("JAVASCRIPT", graph.metadata["language"])
    }
}

