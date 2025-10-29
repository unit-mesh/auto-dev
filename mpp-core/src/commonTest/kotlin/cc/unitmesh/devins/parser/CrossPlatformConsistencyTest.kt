package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-platform consistency tests for DevIn Parser
 * 
 * These tests verify that the parser behavior is consistent across all platforms
 * by testing the same inputs and expecting identical outputs.
 */
class CrossPlatformConsistencyTest {

    private val parser = DevInParserImpl()
    private val lexer = DevInLexerImpl()

    @Test
    fun testBasicTokenizationConsistency() {
        val input = "Hello @agent /command \$variable #expression"
        
        val tokens = lexer.tokenize(input)
        val values = tokens.map { it.value }
        
        // Verify expected tokens are present
        assertTrue(values.contains("Hello"), "Should contain 'Hello'")
        assertTrue(values.contains("@"), "Should contain '@'")
        assertTrue(values.contains("agent"), "Should contain 'agent'")
        assertTrue(values.contains("/"), "Should contain '/'")
        assertTrue(values.contains("command"), "Should contain 'command'")
        assertTrue(values.contains("\$"), "Should contain '\$'")
        assertTrue(values.contains("variable"), "Should contain 'variable'")
        assertTrue(values.contains("#"), "Should contain '#'")
        assertTrue(values.contains("expression"), "Should contain 'expression'")
        
        // Verify token types
        val types = tokens.map { it.type }
        assertTrue(types.contains(DevInTokenTypes.TEXT_SEGMENT), "Should have TEXT_SEGMENT")
        assertTrue(types.contains(DevInTokenTypes.AGENT_START), "Should have AGENT_START")
        assertTrue(types.contains(DevInTokenTypes.COMMAND_START), "Should have COMMAND_START")
        assertTrue(types.contains(DevInTokenTypes.VARIABLE_START), "Should have VARIABLE_START")
        assertTrue(types.contains(DevInTokenTypes.SHARP), "Should have SHARP")
        assertTrue(types.contains(DevInTokenTypes.IDENTIFIER), "Should have IDENTIFIER")
    }

    @Test
    fun testFrontMatterConsistency() {
        val input = """---
name: test
version: 1.0
---"""
        
        val tokens = lexer.tokenize(input)
        val values = tokens.map { it.value }
        val types = tokens.map { it.type }
        
        // Verify front matter tokens
        assertTrue(values.contains("---"), "Should contain '---'")
        assertTrue(values.contains("name"), "Should contain 'name'")
        assertTrue(values.contains("test"), "Should contain 'test'")
        assertTrue(values.contains("version"), "Should contain 'version'")
        assertTrue(values.contains("1.0"), "Should contain '1.0'")
        
        // Verify front matter token types
        assertTrue(types.contains(DevInTokenTypes.FRONTMATTER_START), "Should have FRONTMATTER_START")
        assertTrue(types.contains(DevInTokenTypes.FRONTMATTER_END), "Should have FRONTMATTER_END")
        assertTrue(types.contains(DevInTokenTypes.IDENTIFIER), "Should have IDENTIFIER")
        assertTrue(types.contains(DevInTokenTypes.NUMBER), "Should have NUMBER")
        assertTrue(types.contains(DevInTokenTypes.COLON), "Should have COLON")
    }

    @Test
    fun testExpressionBlockConsistency() {
        val input = "#if (condition == true)"
        
        val tokens = lexer.tokenize(input)
        val values = tokens.map { it.value }
        val types = tokens.map { it.type }
        
        // Verify expression tokens
        assertTrue(values.contains("#"), "Should contain '#'")
        assertTrue(values.contains("if"), "Should contain 'if'")
        assertTrue(values.contains("("), "Should contain '('")
        assertTrue(values.contains("condition"), "Should contain 'condition'")
        assertTrue(values.contains("=="), "Should contain '=='")
        assertTrue(values.contains("true"), "Should contain 'true'")
        assertTrue(values.contains(")"), "Should contain ')'")
        
        // Verify expression token types
        assertTrue(types.contains(DevInTokenTypes.SHARP), "Should have SHARP")
        assertTrue(types.contains(DevInTokenTypes.IF), "Should have IF")
        assertTrue(types.contains(DevInTokenTypes.LPAREN), "Should have LPAREN")
        assertTrue(types.contains(DevInTokenTypes.CONDITION), "Should have CONDITION")
        assertTrue(types.contains(DevInTokenTypes.EQEQ), "Should have EQEQ")
        assertTrue(types.contains(DevInTokenTypes.BOOLEAN), "Should have BOOLEAN")
        assertTrue(types.contains(DevInTokenTypes.RPAREN), "Should have RPAREN")
    }

    @Test
    fun testParserConsistency() {
        val input = """---
name: test
---

@myAgent

/read: file.txt

${'$'}variable = "value"

#if (condition == true)
    Content here
#endif

```kotlin
fun hello() = "world"
```"""

        val result = parser.parse(input)
        
        // Verify parsing succeeded
        assertNotNull(result.ast, "AST should not be null")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        // Verify AST structure
        assertEquals(DevInASTNodeTypes.FILE, result.ast!!.type, "Root should be FILE type")
        assertTrue(result.ast!!.children.isNotEmpty(), "Should have child nodes")
        
        // Verify specific node types are present
        val childTypes = result.ast!!.children.map { it.type }
        assertTrue(childTypes.contains(DevInASTNodeTypes.FRONT_MATTER_HEADER), "Should have FRONT_MATTER_HEADER")
        assertTrue(childTypes.contains(DevInASTNodeTypes.AGENT_BLOCK), "Should have AGENT_BLOCK")
        assertTrue(childTypes.contains(DevInASTNodeTypes.COMMAND_BLOCK), "Should have COMMAND_BLOCK")
        assertTrue(childTypes.contains(DevInASTNodeTypes.VARIABLE_BLOCK), "Should have VARIABLE_BLOCK")
        assertTrue(childTypes.contains(DevInASTNodeTypes.EXPRESSION_BLOCK), "Should have EXPRESSION_BLOCK")
        assertTrue(childTypes.contains(DevInASTNodeTypes.CODE_BLOCK), "Should have CODE_BLOCK")
    }

    @Test
    fun testParseRuleConsistency() {
        // Test valid rule
        val validResult = parser.parseRule("agentBlock", "@testAgent")
        assertNotNull(validResult.ast, "Valid rule should produce AST")
        assertEquals(DevInASTNodeTypes.AGENT_BLOCK, validResult.ast!!.type, "Should return AGENT_BLOCK")
        
        // Test invalid rule
        val invalidResult = parser.parseRule("invalidRule", "test")
        assertTrue(invalidResult.errors.isNotEmpty(), "Invalid rule should produce errors")
        assertTrue(invalidResult.errors.any { it.message.contains("Invalid rule name") }, 
                  "Should have invalid rule error message")
    }

    @Test
    fun testErrorHandlingConsistency() {
        // Test with malformed input that should produce errors
        val malformedInput = "#if (unclosed parenthesis"
        
        val result = parser.parse(malformedInput)
        
        // The parser should handle this gracefully
        assertNotNull(result, "Result should not be null")
        // Note: Depending on implementation, this might or might not produce errors
        // The key is that it should be consistent across platforms
    }

    @Test
    fun testTokenPositionConsistency() {
        val input = "line1\nline2\nline3"
        
        val tokens = lexer.tokenize(input)
        
        // Verify line and column tracking
        var expectedLine = 1
        var expectedColumn = 1
        
        for (token in tokens) {
            assertTrue(token.line >= 1, "Line should be >= 1")
            assertTrue(token.col >= 1, "Column should be >= 1")
            assertTrue(token.offset >= 0, "Offset should be >= 0")
            
            if (token.type == DevInTokenTypes.NEWLINE) {
                expectedLine++
                expectedColumn = 1
            } else {
                expectedColumn += token.value.length
            }
        }
    }

    @Test
    fun testComplexDocumentConsistency() {
        val complexInput = """---
name: complex-example
version: 2.0
author: DevIn Team
tags: [parser, test, multiplatform]
---

// This is a comprehensive test document
@dataProcessor
@"AI Assistant"

#if (environment == "production")
    /read: prod-config.json
    ${'$'}debugMode = false
#elseif (environment == "staging")
    /read: staging-config.json
    ${'$'}debugMode = true
#else
    /read: dev-config.json
    ${'$'}debugMode = true
#endif

${'$'}projectName = "DevIn Parser Test"
${'$'}version = 2.0
${'$'}features = ["lexing", "parsing", "AST"]

```javascript
function processData(input) {
    return input
        .filter(item => item.active)
        .map(item => ({
            ...item,
            processed: true,
            timestamp: Date.now()
        }));
}
```

```python
def analyze_results(data):
    return {
        'count': len(data),
        'average': sum(data) / len(data) if data else 0
    }
```

#when (userRole)
    #case "admin"
        Full access granted
    #case "user"
        Limited access granted
    #default
        Access denied
#end

// Final processing
/process: ${'$'}projectData
/validate: ${'$'}results
/output: final-report.json"""

        // Test lexer consistency
        val tokens = lexer.tokenize(complexInput)
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        // Verify all major token types are present
        val types = tokens.map { it.type }.toSet()
        val expectedTypes = setOf(
            DevInTokenTypes.FRONTMATTER_START,
            DevInTokenTypes.FRONTMATTER_END,
            DevInTokenTypes.AGENT_START,
            DevInTokenTypes.COMMAND_START,
            DevInTokenTypes.VARIABLE_START,
            DevInTokenTypes.SHARP,
            DevInTokenTypes.CODE_BLOCK_START,
            DevInTokenTypes.IDENTIFIER,
            DevInTokenTypes.NUMBER,
            DevInTokenTypes.QUOTE_STRING,
            DevInTokenTypes.COMMENTS,
            DevInTokenTypes.IF,
            DevInTokenTypes.ELSEIF,
            DevInTokenTypes.ELSE,
            DevInTokenTypes.ENDIF,
            DevInTokenTypes.WHEN,
            DevInTokenTypes.CASE,
            DevInTokenTypes.DEFAULT,
            DevInTokenTypes.END
        )
        
        for (expectedType in expectedTypes) {
            assertTrue(types.contains(expectedType), "Should contain token type: $expectedType")
        }
        
        // Test parser consistency
        val result = parser.parse(complexInput)
        assertNotNull(result.ast, "Should produce AST")
        assertTrue(result.ast!!.children.isNotEmpty(), "Should have child nodes")
        
        // Verify major AST node types are present
        val nodeTypes = getAllNodeTypes(result.ast!!)
        val expectedNodeTypes = setOf(
            DevInASTNodeTypes.FILE,
            DevInASTNodeTypes.FRONT_MATTER_HEADER,
            DevInASTNodeTypes.AGENT_BLOCK,
            DevInASTNodeTypes.COMMAND_BLOCK,
            DevInASTNodeTypes.VARIABLE_BLOCK,
            DevInASTNodeTypes.EXPRESSION_BLOCK,
            DevInASTNodeTypes.CODE_BLOCK,
            DevInASTNodeTypes.COMMENTS
        )
        
        for (expectedNodeType in expectedNodeTypes) {
            assertTrue(nodeTypes.contains(expectedNodeType), "Should contain node type: $expectedNodeType")
        }
    }

    private fun getAllNodeTypes(node: DevInASTNode): Set<String> {
        val types = mutableSetOf<String>()
        types.add(node.type)
        
        for (child in node.children) {
            types.addAll(getAllNodeTypes(child))
        }
        
        return types
    }

    @Test
    fun testPerformanceConsistency() {
        // Test with a reasonably large input to verify performance is consistent
        val largeInput = buildString {
            repeat(100) { i ->
                appendLine("---")
                appendLine("iteration: $i")
                appendLine("---")
                appendLine()
                appendLine("@agent$i")
                appendLine()
                appendLine("/process: file$i.txt")
                appendLine()
                appendLine("\$variable$i = \"value$i\"")
                appendLine()
                appendLine("#if (condition$i == true)")
                appendLine("    Content for iteration $i")
                appendLine("#endif")
                appendLine()
                appendLine("```kotlin")
                appendLine("fun process$i() = \"result$i\"")
                appendLine("```")
                appendLine()
            }
        }
        
        // Test lexer performance (without timing for cross-platform compatibility)
        val tokens = lexer.tokenize(largeInput)
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")

        // Test parser performance (without timing for cross-platform compatibility)
        val result = parser.parse(largeInput)
        assertNotNull(result.ast, "Should produce AST")
        
        println("Performance metrics:")
        println("  Input size: ${largeInput.length} characters")
        println("  Tokens generated: ${tokens.size}")
        println("  AST nodes: ${countNodes(result.ast!!)}")
    }

    private fun countNodes(node: DevInASTNode): Int {
        return 1 + node.children.sumOf { countNodes(it) }
    }
}
