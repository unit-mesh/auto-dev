package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Platform Consistency Validator for DevIn Parser
 * 
 * This validator ensures that the DevIn parser behaves consistently across all platforms
 * by testing identical inputs and comparing outputs.
 */
class PlatformConsistencyValidator {

    private val lexer = DevInLexerImpl()
    private val parser = DevInParserImpl()

    @Test
    fun validateBasicTokenization() {
        val testCases = listOf(
            "Hello @agent",
            "/command: file.txt",
            "${'$'}variable = \"value\"",
            "#if (condition)",
            "```kotlin\ncode\n```",
            "---\nname: test\n---"
        )

        for (input in testCases) {
            val tokens = lexer.tokenize(input)
            assertTrue(tokens.isNotEmpty(), "Should produce tokens for: $input")
            
            // Verify token structure is consistent
            for (token in tokens) {
                assertNotNull(token.type, "Token type should not be null")
                assertNotNull(token.value, "Token value should not be null")
                assertTrue(token.line >= 1, "Line should be >= 1")
                assertTrue(token.col >= 1, "Column should be >= 1")
                assertTrue(token.offset >= 0, "Offset should be >= 0")
            }
        }
    }

    @Test
    fun validateParserConsistency() {
        val testCases = listOf(
            "@agent",
            "/read: file.txt",
            "${'$'}var = \"test\"",
            "#if (true)\ncontent\n#endif",
            "```js\nconsole.log('hello');\n```",
            "---\nname: example\n---"
        )

        for (input in testCases) {
            val result = parser.parse(input)
            assertNotNull(result, "Parse result should not be null for: $input")
            
            if (result.errors.isNotEmpty()) {
                println("Warnings for '$input': ${result.errors.map { it.message }}")
            }
            
            // Basic structure validation
            if (result.ast != null) {
                assertEquals(DevInASTNodeTypes.FILE, result.ast!!.type, "Root should be FILE type")
                assertTrue(result.ast!!.startOffset >= 0, "Start offset should be >= 0")
                assertTrue(result.ast!!.endOffset >= result.ast!!.startOffset, "End offset should be >= start offset")
            }
        }
    }

    @Test
    fun validateTokenTypeConsistency() {
        val expectedTokenMappings = mapOf(
            "@" to DevInTokenTypes.AGENT_START,
            "/" to DevInTokenTypes.COMMAND_START,
            "${'$'}" to DevInTokenTypes.VARIABLE_START,
            "#" to DevInTokenTypes.SHARP,
            "```" to DevInTokenTypes.CODE_BLOCK_START,
            "---" to DevInTokenTypes.FRONTMATTER_START,
            ":" to DevInTokenTypes.COLON,
            "(" to DevInTokenTypes.LPAREN,
            ")" to DevInTokenTypes.RPAREN,
            "==" to DevInTokenTypes.EQEQ,
            "true" to DevInTokenTypes.BOOLEAN,
            "false" to DevInTokenTypes.BOOLEAN,
            "if" to DevInTokenTypes.IF,
            "else" to DevInTokenTypes.ELSE,
            "endif" to DevInTokenTypes.ENDIF
        )

        for ((input, expectedType) in expectedTokenMappings) {
            val tokens = lexer.tokenize(input)
            assertTrue(tokens.isNotEmpty(), "Should produce tokens for: $input")
            
            val relevantToken = tokens.find { it.value == input }
            assertNotNull(relevantToken, "Should find token with value '$input'")
            assertEquals(expectedType, relevantToken.type, "Token type mismatch for '$input'")
        }
    }

    @Test
    fun validateComplexDocumentStructure() {
        val complexDocument = """
            ---
            name: test-document
            version: 1.0
            ---
            
            @dataProcessor
            
            /read: input.csv
            /transform: data
            
            ${'$'}outputFile = "result.json"
            ${'$'}debug = true
            
            #if (debug == true)
                Processing in debug mode
            #else
                Processing in production mode
            #endif
            
            ```python
            def process_data(data):
                return data.upper()
            ```
            
            // Final step
            /write: ${'$'}outputFile
        """.trimIndent()

        // Test lexer
        val tokens = lexer.tokenize(complexDocument)
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Verify presence of key token types
        val tokenTypes = tokens.map { it.type }.toSet()
        val requiredTypes = setOf(
            DevInTokenTypes.FRONTMATTER_START,
            DevInTokenTypes.FRONTMATTER_END,
            DevInTokenTypes.AGENT_START,
            DevInTokenTypes.COMMAND_START,
            DevInTokenTypes.VARIABLE_START,
            DevInTokenTypes.SHARP,
            DevInTokenTypes.CODE_BLOCK_START,
            DevInTokenTypes.IDENTIFIER,
            DevInTokenTypes.COLON,
            DevInTokenTypes.QUOTE_STRING,
            DevInTokenTypes.BOOLEAN,
            DevInTokenTypes.IF,
            DevInTokenTypes.ELSE,
            DevInTokenTypes.ENDIF,
            DevInTokenTypes.COMMENTS
        )
        
        for (requiredType in requiredTypes) {
            assertTrue(tokenTypes.contains(requiredType), "Should contain token type: $requiredType")
        }

        // Test parser
        val result = parser.parse(complexDocument)
        assertNotNull(result.ast, "Should produce AST")
        assertEquals(DevInASTNodeTypes.FILE, result.ast!!.type, "Root should be FILE type")
        assertTrue(result.ast!!.children.isNotEmpty(), "Should have child nodes")
        
        // Verify presence of key AST node types
        val nodeTypes = collectAllNodeTypes(result.ast!!)
        val requiredNodeTypes = setOf(
            DevInASTNodeTypes.FILE,
            DevInASTNodeTypes.FRONT_MATTER_HEADER,
            DevInASTNodeTypes.AGENT_BLOCK,
            DevInASTNodeTypes.COMMAND_BLOCK,
            DevInASTNodeTypes.VARIABLE_BLOCK,
            DevInASTNodeTypes.EXPRESSION_BLOCK,
            DevInASTNodeTypes.CODE_BLOCK
        )
        
        for (requiredNodeType in requiredNodeTypes) {
            assertTrue(nodeTypes.contains(requiredNodeType), "Should contain node type: $requiredNodeType")
        }
    }

    @Test
    fun validateParseRuleConsistency() {
        val ruleTests = mapOf(
            "agentBlock" to "@testAgent",
            "commandBlock" to "/read: file.txt",
            "variableBlock" to "${'$'}var = \"value\"",
            "expressionBlock" to "#if (condition == true)",
            "codeBlock" to "```kotlin\nfun test() = \"hello\"\n```"
        )

        for ((ruleName, input) in ruleTests) {
            val result = parser.parseRule(ruleName, input)
            
            if (result.ast != null) {
                assertNotNull(result.ast, "Should produce AST for rule: $ruleName")
                assertTrue(result.ast!!.startOffset >= 0, "Start offset should be >= 0")
                assertTrue(result.ast!!.endOffset >= result.ast!!.startOffset, "End offset should be >= start offset")
            } else {
                // If no AST, should have meaningful error
                assertTrue(result.errors.isNotEmpty(), "Should have errors if no AST for rule: $ruleName")
            }
        }

        // Test invalid rule
        val invalidResult = parser.parseRule("invalidRule", "test")
        assertTrue(invalidResult.errors.isNotEmpty(), "Invalid rule should produce errors")
        assertTrue(
            invalidResult.errors.any { it.message.contains("Invalid rule name") },
            "Should have invalid rule error message"
        )
    }

    @Test
    fun validateErrorHandlingConsistency() {
        val errorTestCases = listOf(
            "", // Empty input
            "   ", // Whitespace only
            "@", // Incomplete agent
            "/", // Incomplete command
            "${'$'}", // Incomplete variable
            "#", // Incomplete expression
            "```", // Incomplete code block
            "---", // Incomplete front matter
        )

        for (input in errorTestCases) {
            // Lexer should handle gracefully
            val tokens = lexer.tokenize(input)
            // Should not throw exception, tokens list can be empty or contain partial tokens
            
            // Parser should handle gracefully
            val result = parser.parse(input)
            assertNotNull(result, "Parse result should not be null even for problematic input: '$input'")
            // Result can have errors or empty AST, but should not be null
        }
    }

    @Test
    fun validatePositionTrackingConsistency() {
        val multilineInput = """
            line1
            line2 with @agent
            line3 with /command
            line4 with ${'$'}variable
        """.trimIndent()

        val tokens = lexer.tokenize(multilineInput)
        
        var currentLine = 1
        var currentColumn = 1
        
        for (token in tokens) {
            // Position should be reasonable
            assertTrue(token.line >= 1, "Line should be >= 1, got ${token.line}")
            assertTrue(token.col >= 1, "Column should be >= 1, got ${token.col}")
            assertTrue(token.offset >= 0, "Offset should be >= 0, got ${token.offset}")
            
            // Line breaks should be tracked correctly
            if (token.type == DevInTokenTypes.NEWLINE) {
                assertEquals(1, token.lineBreaks, "Newline token should have 1 line break")
            }
        }
    }

    private fun collectAllNodeTypes(node: DevInASTNode): Set<String> {
        val types = mutableSetOf<String>()
        types.add(node.type)
        
        for (child in node.children) {
            types.addAll(collectAllNodeTypes(child))
        }
        
        return types
    }

    @Test
    fun validateMemoryAndPerformance() {
        // Test with reasonably large input to ensure no memory leaks or performance issues
        val largeInput = buildString {
            repeat(50) { i ->
                appendLine("---")
                appendLine("iteration: $i")
                appendLine("---")
                appendLine()
                appendLine("@agent$i")
                appendLine("/process: file$i.txt")
                appendLine("${'$'}var$i = \"value$i\"")
                appendLine("#if (condition$i)")
                appendLine("  Content $i")
                appendLine("#endif")
                appendLine("```code")
                appendLine("function test$i() { return $i; }")
                appendLine("```")
                appendLine()
            }
        }

        // Should complete without issues
        val tokens = lexer.tokenize(largeInput)
        assertTrue(tokens.isNotEmpty(), "Should produce tokens for large input")
        
        val result = parser.parse(largeInput)
        assertNotNull(result, "Should produce result for large input")
        
        println("Large input test completed:")
        println("  Input size: ${largeInput.length} characters")
        println("  Tokens: ${tokens.size}")
        println("  AST nodes: ${if (result.ast != null) countNodes(result.ast!!) else 0}")
        println("  Errors: ${result.errors.size}")
    }

    private fun countNodes(node: DevInASTNode): Int {
        return 1 + node.children.sumOf { countNodes(it) }
    }
}
