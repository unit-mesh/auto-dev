package cc.unitmesh.codegraph.parser

import cc.unitmesh.codegraph.CodeGraphFactory
import cc.unitmesh.codegraph.model.CodeElementType
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test to verify that CodeGraph parser handles UTF-8 characters (including emojis) correctly.
 * This tests the fix for the byte-to-char offset bug that caused "Range out of bounds" errors.
 */
class Utf8ParsingTest {
    
    @Test
    fun `should parse Kotlin file with emojis and UTF-8 characters without errors`() {
        val sourceCode = """
            // Test file with UTF-8 characters including emojis
            package cc.unitmesh.test
            
            /**
             * 🤖 Auto-starting analysis with multiple UTF-8 characters
             * This class tests parsing of files with emojis and other multi-byte UTF-8 characters.
             */
            class TestClass {
                // 测试中文注释
                fun helloWorld() {
                    println("Hello 世界 🌍")
                    println("Testing emoji 🚀 parsing")
                }
                
                fun processData() {
                    // 处理数据
                    val message = "Success ✅"
                    val error = "Error ❌"
                    val warning = "Warning ⚠️"
                }
                
                /**
                 * Multi-line comment with emojis
                 * 🔍 Analyzing modified code structure...
                 * ✅ Code analysis complete
                 */
                fun analyze() {
                    // This should not cause "Range out of bounds" error
                    println("Analysis 完成 🎉")
                }
            }
        """.trimIndent()
        
        val parser = CodeGraphFactory.createParser()
        
        // This should not throw "Range out of bounds" error
        val nodes = runSuspend {
            parser.parseNodes(sourceCode, "TestClass.kt", Language.KOTLIN)
        }
        
        // Verify we successfully parsed the file without errors
        // The main goal is to ensure no "Range out of bounds" error occurs
        assertTrue(nodes.isNotEmpty(), "Should have parsed at least one node")
        
        // Check that we found some code elements (the exact count may vary by platform)
        val classNodes = nodes.filter { it.type == CodeElementType.CLASS }
        val methodNodes = nodes.filter { it.type == CodeElementType.METHOD || it.type == CodeElementType.FUNCTION }
        
        assertTrue(classNodes.isNotEmpty() || methodNodes.isNotEmpty(), 
            "Should have found at least some class or method nodes. Found ${nodes.size} nodes total")
    }
    
    @Test
    fun `should parse Java file with UTF-8 characters`() {
        val sourceCode = """
            package com.example;
            
            /**
             * Test class with UTF-8 
             * 测试类 with Chinese characters
             */
            public class Example {
                // Comment with emoji 🚀
                public void test() {
                    System.out.println("Hello 世界");
                }
            }
        """.trimIndent()
        
        val parser = CodeGraphFactory.createParser()
        
        val nodes = runSuspend {
            parser.parseNodes(sourceCode, "Example.java", Language.JAVA)
        }
        
        assertTrue(nodes.isNotEmpty(), "Should have parsed the file")
        val classNode = nodes.find { it.type == CodeElementType.CLASS }
        assertTrue(classNode != null, "Should have found the class")
    }
    
    @Test
    fun `should correctly extract text content with emojis`() {
        // This test verifies that the content extraction works correctly
        val sourceCode = """
            fun emoji() {
                println("🎉")
            }
        """.trimIndent()
        
        val parser = CodeGraphFactory.createParser()
        
        val nodes = runSuspend {
            parser.parseNodes(sourceCode, "test.kt", Language.KOTLIN)
        }
        
        val method = nodes.find { it.type == CodeElementType.METHOD }
        assertTrue(method != null, "Should have found the method")
        assertTrue(method.content.contains("🎉"), "Method content should contain the emoji")
    }
    
    private fun <T> runSuspend(block: suspend () -> T): T {
        // Simple synchronous runner for tests
        return kotlinx.coroutines.runBlocking {
            block()
        }
    }
}

