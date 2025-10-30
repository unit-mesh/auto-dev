package cc.unitmesh.devins.ui.compose.editor.highlighting

import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.token.DevInsTokenType
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 语法高亮集成测试
 * 重点测试 @, /, $ 等特殊符号的识别和高亮
 */
class HighlightingIntegrationTest {
    
    private val highlighter = DevInSyntaxHighlighter()
    
    @Test
    fun `test @ symbol is correctly tokenized and highlighted`() {
        val text = "@clarify"
        
        // 测试 lexer 能正确识别
        val lexer = DevInsLexer(text)
        val tokens = lexer.tokenize()
        
        assertTrue(tokens.any { it.type == DevInsTokenType.AGENT_START }, 
            "Lexer should recognize @ as AGENT_START")
        
        // 测试高亮器能应用样式
        val highlighted = highlighter.highlight(text)
        assertTrue(highlighted.spanStyles.isNotEmpty(), 
            "Highlighter should apply styles to @")
    }
    
    @Test
    fun `test slash command is correctly tokenized and highlighted`() {
        val text = "/file:test.kt"
        
        // 测试 lexer 能正确识别
        val lexer = DevInsLexer(text)
        val tokens = lexer.tokenize()
        
        assertTrue(tokens.any { it.type == DevInsTokenType.COMMAND_START }, 
            "Lexer should recognize / as COMMAND_START")
        
        // 测试高亮器能应用样式
        val highlighted = highlighter.highlight(text)
        assertTrue(highlighted.spanStyles.isNotEmpty(), 
            "Highlighter should apply styles to /")
    }
    
    @Test
    fun `test dollar variable is correctly tokenized and highlighted`() {
        val text = "\$input"
        
        // 测试 lexer 能正确识别
        val lexer = DevInsLexer(text)
        val tokens = lexer.tokenize()
        
        assertTrue(tokens.any { it.type == DevInsTokenType.VARIABLE_START }, 
            "Lexer should recognize \$ as VARIABLE_START")
        
        // 测试高亮器能应用样式
        val highlighted = highlighter.highlight(text)
        assertTrue(highlighted.spanStyles.isNotEmpty(), 
            "Highlighter should apply styles to $")
    }
    
    @Test
    fun `test mixed content with all special symbols`() {
        val text = """
            @clarify /file:test.kt with ${"$"}input
        """.trimIndent()
        
        val lexer = DevInsLexer(text)
        val tokens = lexer.tokenize()
        
        // 应该包含所有特殊符号
        assertTrue(tokens.any { it.type == DevInsTokenType.AGENT_START }, "Should have @")
        assertTrue(tokens.any { it.type == DevInsTokenType.COMMAND_START }, "Should have /")
        assertTrue(tokens.any { it.type == DevInsTokenType.VARIABLE_START }, "Should have $")
        
        // 高亮应该有多个样式
        val highlighted = highlighter.highlight(text)
        assertTrue(highlighted.spanStyles.size >= 3, 
            "Should apply styles to all special symbols")
    }
    
    @Test
    fun `test complex real-world example`() {
        val text = """
            ---
            name: "Test"
            input: "value"
            ---
            
            @code-review Please review this
            /file:src/main/kotlin/Main.kt
            /symbol:UserService.login
            
            Check the ${"$"}input variable
            
            ```kotlin
            fun test() {
                println("Hello")
            }
            ```
        """.trimIndent()
        
        val highlighted = highlighter.highlight(text)
        
        // 应该有多个高亮区域
        assertTrue(highlighted.spanStyles.size >= 5, 
            "Complex example should have multiple highlighted regions")
        
        // 文本应该完整保留
        assertEquals(text, highlighted.text, 
            "Original text should be preserved")
    }
    
    @Test
    fun `test @ at different positions`() {
        val cases = listOf(
            "@start",           // 开头
            "text @middle",     // 中间
            "text @end",        // 结尾
            "@one @two @three"  // 多个
        )
        
        cases.forEach { text ->
            val highlighted = highlighter.highlight(text)
            val atCount = text.count { it == '@' }
            
            assertTrue(highlighted.spanStyles.isNotEmpty(), 
                "Should highlight @ in: $text")
            assertTrue(highlighted.text == text,
                "Text should be preserved in: $text")
        }
    }
    
    @Test
    fun `test slash at different positions`() {
        val cases = listOf(
            "/command",                 // 开头
            "text /command",           // 中间
            "/cmd1 /cmd2",             // 多个
            "/file:path/to/file.kt"    // 路径中的斜杠
        )
        
        cases.forEach { text ->
            val highlighted = highlighter.highlight(text)
            assertTrue(highlighted.spanStyles.isNotEmpty(), 
                "Should highlight / in: $text")
        }
    }
}

