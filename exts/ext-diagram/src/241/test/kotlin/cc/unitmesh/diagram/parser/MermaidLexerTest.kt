package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.parser.mermaid.MermaidLexer
import cc.unitmesh.diagram.parser.mermaid.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MermaidLexerTest {

    @Test
    fun `should tokenize class annotation statement correctly`() {
        val lexer = MermaidLexer("User : Added")
        val tokens = lexer.tokenize()
        
        println("Tokens:")
        tokens.forEach { token ->
            println("  ${token.type}: '${token.value}'")
        }
        
        // Should have: ALPHA, COLON, ALPHA, EOF
        assertTrue(tokens.size >= 4, "Should have at least 4 tokens")
        assertEquals(TokenType.ALPHA, tokens[0].type)
        assertEquals("User", tokens[0].value)
        assertEquals(TokenType.COLON, tokens[1].type)
        assertEquals(":", tokens[1].value)
        assertEquals(TokenType.ALPHA, tokens[2].type)
        assertEquals("Added", tokens[2].value)
        assertEquals(TokenType.EOF, tokens.last().type)
    }

    @Test
    fun `should tokenize full class diagram with annotation`() {
        val input = """
            classDiagram
                class User {
                    +name
                    +email
                }
                User : Added
        """.trimIndent()

        val lexer = MermaidLexer(input)
        val tokens = lexer.tokenize()

        println("Full diagram tokens:")
        tokens.forEach { token ->
            println("  ${token.type}: '${token.value}'")
        }

        // Find the User : Added part
        val userTokenIndex = tokens.indexOfLast { it.type == TokenType.ALPHA && it.value == "User" }
        assertTrue(userTokenIndex >= 0, "Should find User token")

        val colonTokenIndex = userTokenIndex + 1
        assertTrue(colonTokenIndex < tokens.size, "Should have colon after User")
        assertEquals(TokenType.COLON, tokens[colonTokenIndex].type)

        val addedTokenIndex = colonTokenIndex + 1
        assertTrue(addedTokenIndex < tokens.size, "Should have Added after colon")
        assertEquals(TokenType.ALPHA, tokens[addedTokenIndex].type)
        assertEquals("Added", tokens[addedTokenIndex].value)
    }

    @Test
    fun `should test parser with class annotation`() {
        val input = """
            classDiagram
                class User {
                    +name
                    +email
                }
                User : Added
        """.trimIndent()

        val parser = cc.unitmesh.diagram.parser.mermaid.MermaidParser(cc.unitmesh.diagram.parser.mermaid.MermaidLexer(input).tokenize())

        println("Testing parser...")
        val result = parser.parse()

        when (result) {
            is cc.unitmesh.diagram.parser.mermaid.ParseResult.Success -> {
                println("Parse successful!")
                println("AST statements: ${result.ast.statements.map { it::class.simpleName }}")

                val classAnnotationStatements = result.ast.statements.filterIsInstance<cc.unitmesh.diagram.parser.mermaid.ClassAnnotationStatementNode>()
                println("Found ${classAnnotationStatements.size} class annotation statements")
                classAnnotationStatements.forEach { stmt ->
                    println("  Class: ${stmt.className}, Annotation: ${stmt.annotation}")
                }
            }
            is cc.unitmesh.diagram.parser.mermaid.ParseResult.Error -> {
                println("Parse failed with errors:")
                result.errors.forEach { error ->
                    println("  ${error.message}")
                }
            }
        }
    }
}
