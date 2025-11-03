package cc.unitmesh.codegraph.parser.jvm

import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmPythonParserTest {
    
    private val parser = JvmCodeParser()
    
    @Test
    fun `should parse Python class`() = runTest {
        val sourceCode = """
            class Calculator:
                def __init__(self):
                    self.result = 0
                
                def add(self, a, b):
                    return a + b
                
                def subtract(self, a, b):
                    return a - b
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "calculator.py", Language.PYTHON)
        
        assertTrue(nodes.isNotEmpty())
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("Calculator", classNode.name)
        
        val methods = nodes.filter { it.type == CodeElementType.METHOD }
        assertTrue(methods.size >= 2) // __init__, add, subtract
    }
    
    @Test
    fun `should parse Python function`() = runTest {
        val sourceCode = """
            def greet(name):
                return f"Hello, {name}"
            
            def add(a, b):
                return a + b
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "utils.py", Language.PYTHON)
        
        assertTrue(nodes.isNotEmpty())
        val functions = nodes.filter { it.type == CodeElementType.METHOD }
        assertTrue(functions.size >= 2)
    }
    
    @Test
    fun `should parse Python nested class`() = runTest {
        val sourceCode = """
            class Outer:
                def outer_method(self):
                    pass
                
                class Inner:
                    def inner_method(self):
                        pass
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "nested.py", Language.PYTHON)
        
        assertTrue(nodes.isNotEmpty())
        val classes = nodes.filter { it.type == CodeElementType.CLASS }
        assertTrue(classes.size >= 2) // Outer and Inner
    }
    
    @Test
    fun `should parse Python code graph`() = runTest {
        val files = mapOf(
            "calculator.py" to """
                class Calculator:
                    def add(self, a, b):
                        return a + b
            """.trimIndent(),
            "utils.py" to """
                def multiply(a, b):
                    return a * b
            """.trimIndent()
        )
        
        val graph = parser.parseCodeGraph(files, Language.PYTHON)
        
        assertTrue(graph.nodes.isNotEmpty())
        assertEquals("2", graph.metadata["fileCount"])
        assertEquals("PYTHON", graph.metadata["language"])
    }
}

