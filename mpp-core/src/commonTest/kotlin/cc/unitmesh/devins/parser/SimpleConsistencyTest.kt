package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple consistency test to verify basic DevIn parser functionality across platforms
 */
class SimpleConsistencyTest {

    private val lexer = DevInLexerImpl()
    private val parser = DevInParserImpl()

    @Test
    fun testBasicAgentTokenization() {
        val input = "@myAgent"
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val agentStartToken = tokens.find { it.type == DevInTokenTypes.AGENT_START }
        assertNotNull(agentStartToken, "Should have AGENT_START token")
        assertEquals("@", agentStartToken.value, "AGENT_START token should be '@'")
        
        val identifierToken = tokens.find { it.type == DevInTokenTypes.IDENTIFIER }
        assertNotNull(identifierToken, "Should have IDENTIFIER token")
        assertEquals("myAgent", identifierToken.value, "IDENTIFIER token should be 'myAgent'")
    }

    @Test
    fun testBasicCommandTokenization() {
        val input = "/read: file.txt"
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val commandStartToken = tokens.find { it.type == DevInTokenTypes.COMMAND_START }
        assertNotNull(commandStartToken, "Should have COMMAND_START token")
        assertEquals("/", commandStartToken.value, "COMMAND_START token should be '/'")
        
        val identifierToken = tokens.find { it.type == DevInTokenTypes.IDENTIFIER }
        assertNotNull(identifierToken, "Should have IDENTIFIER token")
        assertEquals("read", identifierToken.value, "IDENTIFIER token should be 'read'")
        
        val colonToken = tokens.find { it.type == DevInTokenTypes.COLON }
        assertNotNull(colonToken, "Should have COLON token")
        assertEquals(":", colonToken.value, "COLON token should be ':'")
    }

    @Test
    fun testBasicVariableTokenization() {
        val input = "${'$'}variable = \"value\""
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val variableStartToken = tokens.find { it.type == DevInTokenTypes.VARIABLE_START }
        assertNotNull(variableStartToken, "Should have VARIABLE_START token")
        assertEquals("$", variableStartToken.value, "VARIABLE_START token should be '\$'")
        
        val identifierToken = tokens.find { it.type == DevInTokenTypes.IDENTIFIER }
        assertNotNull(identifierToken, "Should have IDENTIFIER token")
        assertEquals("variable", identifierToken.value, "IDENTIFIER token should be 'variable'")
        
        val stringToken = tokens.find { it.type == DevInTokenTypes.QUOTE_STRING }
        assertNotNull(stringToken, "Should have QUOTE_STRING token")
        assertEquals("\"value\"", stringToken.value, "QUOTE_STRING token should be '\"value\"'")
    }

    @Test
    fun testBasicExpressionTokenization() {
        val input = "#if (condition == true)"
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val sharpToken = tokens.find { it.type == DevInTokenTypes.SHARP }
        assertNotNull(sharpToken, "Should have SHARP token")
        assertEquals("#", sharpToken.value, "SHARP token should be '#'")
        
        val ifToken = tokens.find { it.type == DevInTokenTypes.IF }
        assertNotNull(ifToken, "Should have IF token")
        assertEquals("if", ifToken.value, "IF token should be 'if'")
        
        val booleanToken = tokens.find { it.type == DevInTokenTypes.BOOLEAN }
        assertNotNull(booleanToken, "Should have BOOLEAN token")
        assertEquals("true", booleanToken.value, "BOOLEAN token should be 'true'")
        
        val eqToken = tokens.find { it.type == DevInTokenTypes.EQEQ }
        assertNotNull(eqToken, "Should have EQEQ token")
        assertEquals("==", eqToken.value, "EQEQ token should be '=='")
    }

    @Test
    fun testBasicFrontMatterTokenization() {
        val input = "---\nname: test\n---"
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val frontMatterStartTokens = tokens.filter { it.type == DevInTokenTypes.FRONTMATTER_START }
        assertEquals(1, frontMatterStartTokens.size, "Should have exactly one FRONTMATTER_START token")
        assertEquals("---", frontMatterStartTokens[0].value, "FRONTMATTER_START token should be '---'")
        
        val frontMatterEndTokens = tokens.filter { it.type == DevInTokenTypes.FRONTMATTER_END }
        assertEquals(1, frontMatterEndTokens.size, "Should have exactly one FRONTMATTER_END token")
        assertEquals("---", frontMatterEndTokens[0].value, "FRONTMATTER_END token should be '---'")
        
        val identifierTokens = tokens.filter { it.type == DevInTokenTypes.IDENTIFIER }
        assertTrue(identifierTokens.any { it.value == "name" }, "Should have 'name' identifier")
        assertTrue(identifierTokens.any { it.value == "test" }, "Should have 'test' identifier")
    }

    @Test
    fun testBasicCodeBlockTokenization() {
        val input = "```javascript\nconsole.log('hello');\n```"
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val codeBlockTokens = tokens.filter { it.type == DevInTokenTypes.CODE_BLOCK_START }
        assertTrue(codeBlockTokens.isNotEmpty(), "Should have CODE_BLOCK_START token(s)")
        assertEquals("```", codeBlockTokens[0].value, "First CODE_BLOCK_START token should be '```'")
        
        val identifierTokens = tokens.filter { it.type == DevInTokenTypes.IDENTIFIER }
        assertTrue(identifierTokens.any { it.value == "javascript" }, "Should have 'javascript' identifier")
    }

    @Test
    fun testBasicParserFunctionality() {
        val input = "@agent"
        val result = parser.parse(input)
        
        assertNotNull(result, "Parse result should not be null")
        assertNotNull(result.ast, "AST should not be null")
        assertEquals(DevInASTNodeTypes.FILE, result.ast!!.type, "Root should be FILE type")
        assertTrue(result.ast!!.children.isNotEmpty(), "Should have child nodes")
        
        val agentBlocks = result.ast!!.children.filter { it.type == DevInASTNodeTypes.AGENT_BLOCK }
        assertTrue(agentBlocks.isNotEmpty(), "Should have at least one AGENT_BLOCK")
    }

    @Test
    fun testParseRuleBasicFunctionality() {
        val agentResult = parser.parseRule("agentBlock", "@testAgent")
        if (agentResult.ast != null) {
            assertEquals(DevInASTNodeTypes.AGENT_BLOCK, agentResult.ast!!.type, "Should return AGENT_BLOCK")
        }
        
        val invalidResult = parser.parseRule("invalidRule", "test")
        assertTrue(invalidResult.errors.isNotEmpty(), "Invalid rule should produce errors")
    }

    @Test
    fun testTokenPositionTracking() {
        val input = "line1\nline2"
        val tokens = lexer.tokenize(input)
        
        for (token in tokens) {
            assertTrue(token.line >= 1, "Line should be >= 1")
            assertTrue(token.col >= 1, "Column should be >= 1")
            assertTrue(token.offset >= 0, "Offset should be >= 0")
        }
        
        val newlineTokens = tokens.filter { it.type == DevInTokenTypes.NEWLINE }
        assertTrue(newlineTokens.isNotEmpty(), "Should have newline tokens")
        assertEquals(1, newlineTokens[0].lineBreaks, "Newline token should have 1 line break")
    }

    @Test
    fun testErrorHandling() {
        // Test with empty input
        val emptyResult = parser.parse("")
        assertNotNull(emptyResult, "Empty input should not return null")
        
        // Test with whitespace only
        val whitespaceResult = parser.parse("   ")
        assertNotNull(whitespaceResult, "Whitespace input should not return null")
        
        // Test with incomplete tokens
        val incompleteResults = listOf(
            parser.parse("@"),
            parser.parse("/"),
            parser.parse("$"),
            parser.parse("#"),
            parser.parse("```"),
            parser.parse("---")
        )
        
        for (result in incompleteResults) {
            assertNotNull(result, "Incomplete input should not return null")
        }
    }

    @Test
    fun testComplexDocumentStructure() {
        val complexInput = """
            ---
            name: test
            ---
            
            @agent
            /command: file.txt
            ${'$'}var = "value"
            
            #if (condition)
                content
            #endif
            
            ```code
            test
            ```
        """.trimIndent()
        
        val tokens = lexer.tokenize(complexInput)
        assertTrue(tokens.isNotEmpty(), "Should produce tokens for complex input")
        
        val result = parser.parse(complexInput)
        assertNotNull(result.ast, "Should produce AST for complex input")
        assertEquals(DevInASTNodeTypes.FILE, result.ast!!.type, "Root should be FILE type")
        assertTrue(result.ast!!.children.isNotEmpty(), "Should have child nodes")
        
        // Verify major components are present
        val childTypes = result.ast!!.children.map { it.type }.toSet()
        assertTrue(childTypes.contains(DevInASTNodeTypes.FRONT_MATTER_HEADER), "Should have front matter")
        assertTrue(childTypes.contains(DevInASTNodeTypes.AGENT_BLOCK), "Should have agent block")
        assertTrue(childTypes.contains(DevInASTNodeTypes.COMMAND_BLOCK), "Should have command block")
        assertTrue(childTypes.contains(DevInASTNodeTypes.VARIABLE_BLOCK), "Should have variable block")
        assertTrue(childTypes.contains(DevInASTNodeTypes.EXPRESSION_BLOCK), "Should have expression block")
        assertTrue(childTypes.contains(DevInASTNodeTypes.CODE_BLOCK), "Should have code block")
    }
}
