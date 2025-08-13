package cc.unitmesh.diagram.parser

import cc.unitmesh.diagram.parser.mermaid.MermaidLexer
import cc.unitmesh.diagram.parser.mermaid.TokenType
import cc.unitmesh.diagram.parser.mermaid.MermaidToken
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

        val lexer = cc.unitmesh.diagram.parser.mermaid.MermaidLexer(input)
        val tokens = lexer.tokenize()

        // Verify lexer produces expected tokens
        val expectedTokenTypes = listOf(
            TokenType.CLASS_DIAGRAM,
            TokenType.NEWLINE,
            TokenType.CLASS,
            TokenType.ALPHA, // User
            TokenType.STRUCT_START,
            TokenType.MEMBER, // +name
            TokenType.MEMBER, // +email
            TokenType.STRUCT_STOP,
            TokenType.NEWLINE,
            TokenType.ALPHA, // User
            TokenType.COLON,
            TokenType.ALPHA, // Added
            TokenType.EOF
        )

        assertEquals(expectedTokenTypes.size, tokens.size, "Token count mismatch")
        for (i in expectedTokenTypes.indices) {
            assertEquals(expectedTokenTypes[i], tokens[i].type, "Token type mismatch at position $i")
        }

        // Verify specific token values
        assertEquals("User", tokens[3].value, "First User token value")
        assertEquals("User", tokens[9].value, "Second User token value")
        assertEquals("Added", tokens[11].value, "Annotation token value")

        val parser = cc.unitmesh.diagram.parser.mermaid.MermaidParser(tokens)
        val result = parser.parse()

        // Verify parsing succeeded
        assertTrue(result is cc.unitmesh.diagram.parser.mermaid.ParseResult.Success, "Parser should succeed")

        val successResult = result as cc.unitmesh.diagram.parser.mermaid.ParseResult.Success

        // Verify AST contains expected statements
        assertEquals(2, successResult.ast.statements.size, "Should have 2 statements")
        assertTrue(successResult.ast.statements[0] is cc.unitmesh.diagram.parser.mermaid.ClassStatementNode, "First statement should be ClassStatementNode")
        assertTrue(successResult.ast.statements[1] is cc.unitmesh.diagram.parser.mermaid.ClassAnnotationStatementNode, "Second statement should be ClassAnnotationStatementNode")

        // Verify class annotation statement details
        val classAnnotationStatements = successResult.ast.statements.filterIsInstance<cc.unitmesh.diagram.parser.mermaid.ClassAnnotationStatementNode>()
        assertEquals(1, classAnnotationStatements.size, "Should have exactly 1 class annotation statement")

        val annotationStmt = classAnnotationStatements[0]
        assertEquals("User", annotationStmt.className, "Class name should be 'User'")
        assertEquals("Added", annotationStmt.annotation, "Annotation should be 'Added'")

        // Test the class diagram parser
        val classDiagramParser = cc.unitmesh.diagram.parser.MermaidClassDiagramParser()
        val diagramResult = classDiagramParser.parse(successResult.ast)

        // Verify diagram parsing results
        assertTrue(diagramResult.graphAttributes.containsKey("User_change"), "Should contain User_change attribute")
        assertEquals("Added", diagramResult.graphAttributes["User_change"], "User_change should be 'Added'")
        assertEquals("mermaid_class_diagram", diagramResult.graphAttributes["type"], "Type should be mermaid_class_diagram")

        assertEquals(1, diagramResult.entities.size, "Should have 1 entity")
        assertEquals("User", diagramResult.entities.first().getName(), "Entity name should be 'User'")
    }
}
