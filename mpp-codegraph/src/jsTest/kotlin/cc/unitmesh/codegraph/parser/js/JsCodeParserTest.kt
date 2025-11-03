package cc.unitmesh.codegraph.parser.js

import cc.unitmesh.codegraph.model.CodeElementType
import cc.unitmesh.codegraph.parser.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsCodeParserTest {
    
    private val parser = JsCodeParser()
    
    @Test
    fun testParserInitialization() = runTest {
        // Just test that initialization doesn't throw
        parser.initialize()
    }
    
    @Test
    fun testParseSimpleJavaScriptFunction() = runTest {
        val sourceCode = """
            function sayHello() {
                console.log("Hello");
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "hello.js", Language.JAVASCRIPT)
        
        assertTrue(nodes.isNotEmpty())
        
        val functionNode = nodes.find { it.type == CodeElementType.METHOD }
        assertTrue(functionNode != null)
        assertEquals("sayHello", functionNode.name)
    }
    
    @Test
    fun testParseTypeScriptClass() = runTest {
        val sourceCode = """
            class HelloWorld {
                private message: string;
                
                sayHello(): void {
                    console.log("Hello");
                }
            }
        """.trimIndent()
        
        val nodes = parser.parseNodes(sourceCode, "HelloWorld.ts", Language.TYPESCRIPT)
        
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null)
        assertEquals("HelloWorld", classNode.name)
    }
}

