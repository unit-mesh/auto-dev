package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DevInLexerTest {
    
    @Test
    fun testBasicTokenization() {
        val lexer = DevInLexerImpl()
        val input = "Hello @agent /command \$variable #expression"
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Check that we have the expected token types
        val tokenTypes = tokens.map { it.type }
        assertTrue(tokenTypes.contains("TEXT_SEGMENT") || tokenTypes.contains("textSegment"))
        assertTrue(tokenTypes.contains("AGENT_START") || tokenTypes.contains("agentStart"))
        assertTrue(tokenTypes.contains("COMMAND_START") || tokenTypes.contains("commandStart"))
        assertTrue(tokenTypes.contains("VARIABLE_START") || tokenTypes.contains("variableStart"))
        assertTrue(tokenTypes.contains("SHARP") || tokenTypes.contains("sharp"))
    }
    
    @Test
    fun testAgentBlock() {
        val lexer = DevInLexerImpl()
        val input = "@myAgent"
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.size >= 2, "Should have at least agent start and identifier")
        
        val firstToken = tokens[0]
        assertEquals("@", firstToken.value)
        
        val secondToken = tokens[1]
        assertEquals("myAgent", secondToken.value)
    }
    
    @Test
    fun testCommandBlock() {
        val lexer = DevInLexerImpl()
        val input = "/read: file.txt #L1-L10"
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Should contain command start, identifier, colon, command prop, sharp, line info
        val values = tokens.map { it.value }
        assertTrue(values.contains("/"))
        assertTrue(values.contains("read"))
        assertTrue(values.contains(":"))
        assertTrue(values.contains("file.txt"))
        assertTrue(values.contains("#"))
    }
    
    @Test
    fun testVariableBlock() {
        val lexer = DevInLexerImpl()
        val input = "\$myVariable"
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.size >= 2, "Should have at least variable start and identifier")
        
        val firstToken = tokens[0]
        assertEquals("$", firstToken.value)
        
        val secondToken = tokens[1]
        assertEquals("myVariable", secondToken.value)
    }
    
    @Test
    fun testCodeBlock() {
        val lexer = DevInLexerImpl()
        val input = """
            ```javascript
            console.log("Hello World");
            ```
        """.trimIndent()
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Should contain code block start and end
        val values = tokens.map { it.value }
        assertTrue(values.any { it.startsWith("```") || it.contains("javascript") })
    }
    
    @Test
    fun testFrontMatter() {
        val lexer = DevInLexerImpl()
        val input = """
            ---
            name: test
            version: 1.0
            ---
        """.trimIndent()
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Should contain front matter markers
        val values = tokens.map { it.value }
        assertTrue(values.contains("---"))
        assertTrue(values.contains("name"))
        assertTrue(values.contains("test"))
    }
    
    @Test
    fun testComments() {
        val lexer = DevInLexerImpl()
        val input = """
            // This is a line comment
            /* This is a block comment */
            [This is a content comment] Some text
        """.trimIndent()
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Should contain comment tokens
        val commentTokens = tokens.filter { 
            it.type.contains("comment", ignoreCase = true) ||
            it.type.contains("Comment", ignoreCase = true)
        }
        assertTrue(commentTokens.isNotEmpty(), "Should have comment tokens")
    }
    
    @Test
    fun testExpressionBlock() {
        val lexer = DevInLexerImpl()
        val input = "#if (condition == true)"
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val values = tokens.map { it.value }
        assertTrue(values.contains("#"))
        assertTrue(values.contains("if"))
        assertTrue(values.contains("("))
        assertTrue(values.contains("condition"))
        assertTrue(values.contains("=="))
        assertTrue(values.contains("true"))
        assertTrue(values.contains(")"))
    }
    
    @Test
    fun testComplexExample() {
        val lexer = DevInLexerImpl()
        val input = """
            ---
            name: example
            agent: myAgent
            ---
            
            Hello @myAgent, please /read: file.txt and process ${'$'}data.
            
            ```javascript
            console.log("Processing data");
            ```
            
            #if (data.length > 0)
                Process the data
            #end
        """.trimIndent()
        
        val tokens = lexer.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Verify we have a good mix of token types
        val tokenTypes = tokens.map { it.type }.toSet()
        assertTrue(tokenTypes.size > 5, "Should have multiple token types")
        
        println("Token types found: $tokenTypes")
        println("Total tokens: ${tokens.size}")
        
        // Print first few tokens for debugging
        tokens.take(10).forEach { token ->
            println("Token: type=${token.type}, value='${token.value}', line=${token.line}, col=${token.col}")
        }
    }
}
