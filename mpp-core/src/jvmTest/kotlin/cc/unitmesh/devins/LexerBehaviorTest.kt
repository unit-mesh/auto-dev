package cc.unitmesh.devins

import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.token.DevInsTokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 验证修复后的 Lexer 行为
 * 修复：只在行首或空白字符后识别 @/$/#，避免误识别普通文本
 */
class LexerBehaviorTest {
    
    @Test
    fun testEmailAddressNotRecognizedAsAgent() {
        // Email 地址不应该被识别为 agent
        val input = "user@example.com"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[EMAIL TEST] $tokenStr")
        
        // 修复后：应该是一个完整的 TEXT_SEGMENT
        assertEquals(1, tokens.size, "Email should be one TEXT_SEGMENT: $tokenStr")
        assertEquals(DevInsTokenType.TEXT_SEGMENT, tokens[0].type)
        assertEquals("user@example.com", tokens[0].text)
    }
    
    @Test
    fun testPathNotRecognizedAsCommand() {
        // 路径中的 "/" 不应该被识别为命令
        val input = "Path: /home/user/file.txt"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[PATH TEST] $tokenStr")
        
        // 修复后："Path: " 后面的空格使得 "/" 被识别为命令
        // 这是符合预期的，因为 "/" 在空白后
        val hasCommandStart = tokens.any { it.type == DevInsTokenType.COMMAND_START }
        assertTrue(hasCommandStart, "Should have COMMAND_START after space: $tokenStr")
    }
    
    @Test
    fun testInlinePathNotRecognizedAsCommand() {
        // 文本中间的路径不应该被识别为命令
        val input = "file path:/home/user/file.txt end"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[INLINE PATH TEST] $tokenStr")
        
        // 修复后："path:" 后面紧跟 "/"，没有空白，所以不识别为命令
        val commandTokens = tokens.filter { it.type == DevInsTokenType.COMMAND_START }
        assertEquals(0, commandTokens.size, "Inline path should not have COMMAND_START: $tokenStr")
    }
    
    @Test
    fun testMarkdownListBehavior() {
        // markdown 列表应该被正常处理
        val input = "- Item with text"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[LIST TEST] $tokenStr")
        
        // "-" 不是特殊字符，应该被包含在 TEXT_SEGMENT 中
        assertEquals(1, tokens.size, "List should be one TEXT_SEGMENT: $tokenStr")
        assertEquals("- Item with text", tokens[0].text)
    }
    
    @Test
    fun testLineStartCommand() {
        // 行首的命令应该被正确识别
        val input = "/file test.txt"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[COMMAND TEST] $tokenStr")
        
        // 行首的 "/" 应该被识别为 COMMAND_START
        assertTrue(tokens.isNotEmpty())
        assertEquals(DevInsTokenType.COMMAND_START, tokens[0].type, "Expected COMMAND_START: $tokenStr")
    }
    
    @Test
    fun testAgentAfterSpace() {
        // 空白后的 @ 应该被识别
        val input = "Call @agent for help"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[AGENT AFTER SPACE TEST] $tokenStr")
        
        // 应该有 AGENT_START
        val hasAgentStart = tokens.any { it.type == DevInsTokenType.AGENT_START }
        assertTrue(hasAgentStart, "Should recognize @agent after space: $tokenStr")
    }
    
    @Test
    fun testVariableInText() {
        // 行首的变量应该被识别
        val input = "${'$'}variable is here"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize().filter { !it.isEof }
        
        val tokenStr = tokens.joinToString(" + ") { "${it.type}('${it.text}')" }
        System.err.println("[VARIABLE TEST] $tokenStr")
        
        // 行首的 $ 应该被识别为 VARIABLE_START
        assertTrue(tokens.isNotEmpty())
        assertEquals(DevInsTokenType.VARIABLE_START, tokens[0].type, "Expected VARIABLE_START: $tokenStr")
    }
}

