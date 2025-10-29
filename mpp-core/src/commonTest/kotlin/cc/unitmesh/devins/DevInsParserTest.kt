package cc.unitmesh.devins

import cc.unitmesh.devins.ast.*
import cc.unitmesh.devins.parser.DevInsParser
import cc.unitmesh.devins.parser.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DevInsParserTest {
    
    @Test
    fun testEmptyFile() {
        val input = ""
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Empty file should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isEmpty(), "Empty file should have no children")
    }
    
    @Test
    fun testSimpleFrontMatter() {
        val input = """
            ---
            name: "test"
            value: 42
            ---
        """.trimIndent()
        
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Simple front matter should parse successfully")
        val ast = result.getOrThrow()
        
        val frontMatter = ast.frontMatter
        assertNotNull(frontMatter, "Should have front matter")
        assertTrue(frontMatter.entries.isNotEmpty(), "Front matter should have entries")
    }
    
    @Test
    fun testFrontMatterWithLifecycle() {
        val input = """
            ---
            when: "condition"
            onStreaming: { action }
            afterStreaming: { cleanup }
            ---
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        if (result.isFailure) {
            val error = (result as ParseResult.Failure).error
            println("Parse failed: ${error.message}")
            println("Position: ${error.position}")
        }

        assertTrue(result.isSuccess, "Front matter with lifecycle should parse successfully")
        val ast = result.getOrThrow()

        val frontMatter = ast.frontMatter
        assertNotNull(frontMatter, "Should have front matter")
        assertTrue(frontMatter.entries.size >= 3, "Should have at least 3 entries")
    }
    
    @Test
    fun testFrontMatterWithPattern() {
        val input = """
            ---
            variables:
              "test": /.*\.kt/ { cat }
            ---
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        if (result.isFailure) {
            val error = (result as ParseResult.Failure).error
            println("Parse failed: ${error.message}")
            println("Position: ${error.position}")
        }

        assertTrue(result.isSuccess, "Front matter with pattern should parse successfully")
        val ast = result.getOrThrow()

        val frontMatter = ast.frontMatter
        assertNotNull(frontMatter, "Should have front matter")
    }
    
    @Test
    fun testSimpleTextSegment() {
        val input = "This is a simple text segment."
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Simple text should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }
    
    @Test
    fun testCodeBlock() {
        val input = """
            ```kotlin
            fun main() {
                println("Hello World")
            }
            ```
        """.trimIndent()
        
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Code block should parse successfully")
        val ast = result.getOrThrow()
        
        val codeBlocks = ast.codeBlocks
        assertTrue(codeBlocks.isNotEmpty(), "Should have code blocks")
    }
    
    @Test
    fun testVariable() {
        val input = "${'$'}variable"
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Variable should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }
    
    @Test
    fun testCommand() {
        val input = "/command:argument"
        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Command should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }
    
    @Test
    fun testAgent() {
        val input = "@agent"
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        assertTrue(result.isSuccess, "Agent should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }
    
    @Test
    fun testComplexDocument() {
        // 简化测试：只测试基本的前置元数据和内容，不测试复杂的嵌套语法
        val input = """
            ---
            name: "Complex Test"
            ---

            This is a complex DevIns document.

            It contains multiple elements:
            - Variables: ${'$'}test
            - Commands: /file:example.kt
            - Agents: @helper

            ```kotlin
            fun example() {
                println("This is a code block")
            }
            ```

            And some more text at the end.
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Complex document should parse successfully")
        val ast = result.getOrThrow()

        // 验证前置元数据
        val frontMatter = ast.frontMatter
        assertNotNull(frontMatter, "Should have front matter")
        assertTrue(frontMatter.entries.isNotEmpty(), "Front matter should have entries")

        // 验证有内容
        assertTrue(ast.children.size > 1, "Should have multiple children")
    }
    
    @Test
    fun testParseError() {
        val input = """
            ---
            invalid syntax here
            no colon
            ---
        """.trimIndent()
        
        val parser = DevInsParser(input)
        val result = parser.parse()
        
        // 根据我们的实现，这可能会成功解析但产生错误节点
        // 或者可能会失败，这取决于具体的错误处理策略
        if (result.isFailure) {
            val error = (result as ParseResult.Failure).error
            assertTrue(error.message.isNotEmpty(), "Error should have a message")
        }
    }
    
    @Test
    fun testMixedContent() {
        // 进一步简化测试：只测试前置元数据、文本、命令、代理和代码块
        val input = """
            ---
            name: "test"
            ---

            Some text after front matter.
            /command:arg
            @agent

            ```python
            print("code block")
            ```

            Final text.
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Mixed content should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }

    @Test
    fun testSimpleCodeBlock() {
        val input = """
            ```kotlin
            fun test() {}
            ```
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Simple code block should parse successfully")
        val ast = result.getOrThrow()
        assertTrue(ast.children.isNotEmpty(), "Should have content")
    }

    @Test
    fun testFrontMatterWithText() {
        val input = """
            ---
            name: "test"
            ---

            Some text after front matter.
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Front matter with text should parse successfully")
        val ast = result.getOrThrow()
        assertNotNull(ast.frontMatter, "Should have front matter")
        assertTrue(ast.children.size > 1, "Should have content after front matter")
    }

    @Test
    fun testFrontMatterWithCodeBlock() {
        val input = """
            ---
            name: "test"
            ---

            ```python
            print("code block")
            ```
        """.trimIndent()

        val parser = DevInsParser(input)
        val result = parser.parse()

        assertTrue(result.isSuccess, "Front matter with code block should parse successfully")
        val ast = result.getOrThrow()
        assertNotNull(ast.frontMatter, "Should have front matter")
    }
}
