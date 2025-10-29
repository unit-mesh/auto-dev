package cc.unitmesh.devins

import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.token.DevInsTokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevInsLexerTest {
    
    @Test
    fun testBasicTokens() {
        // 在 YYINITIAL 状态下，@ / $ 在行首会被识别为特殊符号
        val input = "@ / $"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        // @ 在行首，被识别为 AGENT_START
        // 然后 " / $" 继续处理，空格被识别为 TEXT_SEGMENT 的一部分
        // / 被识别为 COMMAND_START
        // 然后 " $" 继续处理，空格被识别为 TEXT_SEGMENT 的一部分
        // $ 被识别为 VARIABLE_START
        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 应该有: AGENT_START(@), TEXT_SEGMENT( ), COMMAND_START(/), TEXT_SEGMENT( ), VARIABLE_START($)
        // 或者如果空格被跳过: AGENT_START(@), COMMAND_START(/), VARIABLE_START($)
        assertTrue(nonEofTokens.any { it.type == DevInsTokenType.AGENT_START })
        assertTrue(nonEofTokens.any { it.type == DevInsTokenType.COMMAND_START })
        assertTrue(nonEofTokens.any { it.type == DevInsTokenType.VARIABLE_START })
    }

    @Test
    fun testKeywords() {
        // 在 YYINITIAL 状态下，关键字不会被单独识别，而是作为 TEXT_SEGMENT
        val input = "when case default if else"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }

    @Test
    fun testOperators() {
        // 在 YYINITIAL 状态下，操作符不会被单独识别，而是作为 TEXT_SEGMENT
        val input = ": == != < > <= >= && ||"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }

    @Test
    fun testBrackets() {
        // 在 YYINITIAL 状态下，括号不会被单独识别，而是作为 TEXT_SEGMENT
        val input = "( ) [ ] { }"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }

    @Test
    fun testIdentifiers() {
        // 在 YYINITIAL 状态下，标识符不会被单独识别，而是作为 TEXT_SEGMENT
        val input = "identifier test_name kebab-case"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }

    @Test
    fun testNumbers() {
        // 在 YYINITIAL 状态下，数字不会被单独识别，而是作为 TEXT_SEGMENT
        val input = "123 456 0"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }

    @Test
    fun testQuotedStrings() {
        // 在 YYINITIAL 状态下，引用字符串不会被单独识别，而是作为 TEXT_SEGMENT
        val input = "\"hello world\" 'single quoted'"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
    }
    
    @Test
    fun testFrontMatter() {
        val input = "---\nname: test\n---"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()
        
        val frontMatterStart = tokens.find { it.type == DevInsTokenType.FRONTMATTER_START }
        val frontMatterEnd = tokens.find { it.type == DevInsTokenType.FRONTMATTER_END }
        
        assertTrue(frontMatterStart != null, "Should find FRONTMATTER_START token")
        assertTrue(frontMatterEnd != null, "Should find FRONTMATTER_END token")
        assertEquals("---", frontMatterStart!!.text)
        assertEquals("---", frontMatterEnd!!.text)
    }
    
    @Test
    fun testCodeBlock() {
        val input = "```kotlin\nfun main() {}\n```"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()
        
        val codeBlockStart = tokens.find { it.type == DevInsTokenType.CODE_BLOCK_START }
        val codeBlockEnd = tokens.find { it.type == DevInsTokenType.CODE_BLOCK_END }
        val languageId = tokens.find { it.type == DevInsTokenType.LANGUAGE_ID }
        
        assertTrue(codeBlockStart != null, "Should find CODE_BLOCK_START token")
        assertTrue(codeBlockEnd != null, "Should find CODE_BLOCK_END token")
        assertEquals("```", codeBlockStart!!.text)
        assertEquals("```", codeBlockEnd!!.text)
    }
    
    @Test
    fun testComments() {
        val input = "// line comment\n/* block comment */"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()
        
        val lineComment = tokens.find { it.type == DevInsTokenType.COMMENTS }
        val blockComment = tokens.find { it.type == DevInsTokenType.BLOCK_COMMENT }
        
        assertTrue(lineComment != null, "Should find line comment")
        assertTrue(blockComment != null, "Should find block comment")
        assertTrue(lineComment!!.text.startsWith("//"))
        assertTrue(blockComment!!.text.startsWith("/*"))
    }
    
    @Test
    fun testNewlines() {
        val input = "line1\nline2\r\nline3"
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()
        
        val newlineTokens = tokens.filter { it.type == DevInsTokenType.NEWLINE }
        assertTrue(newlineTokens.isNotEmpty(), "Should find newline tokens")
    }
    
    @Test
    fun testTextSegments() {
        // 在 YYINITIAL 状态下，整个文本应该被识别为一个 TEXT_SEGMENT
        val input = "This is a text segment."
        val lexer = DevInsLexer(input)
        val tokens = lexer.tokenize()

        val nonEofTokens = tokens.filter { it.type != DevInsTokenType.EOF }

        // 整个字符串应该被识别为一个 TEXT_SEGMENT
        assertEquals(1, nonEofTokens.size)
        assertEquals(DevInsTokenType.TEXT_SEGMENT, nonEofTokens[0].type)
        assertEquals("This is a text segment.", nonEofTokens[0].text)
    }
}
