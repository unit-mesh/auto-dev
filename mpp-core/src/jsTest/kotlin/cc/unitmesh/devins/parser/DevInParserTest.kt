package cc.unitmesh.devins.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DevInParserTest {
    
    @Test
    fun testBasicParsing() {
        val parser = DevInParserImpl()
        val input = "Hello World"
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertEquals("FILE", result.ast!!.type)
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }
    
    @Test
    fun testAgentBlockParsing() {
        val parser = DevInParserImpl()
        val input = "@myAgent"
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        assertTrue(children.isNotEmpty(), "Should have child nodes")
        
        val agentBlock = children.find { it.type == "AGENT_BLOCK" }
        assertNotNull(agentBlock, "Should contain an agent block")
        
        if (agentBlock is DevInAgentBlockNode) {
            assertEquals("myAgent", agentBlock.agentId)
        }
    }
    
    @Test
    fun testCommandBlockParsing() {
        val parser = DevInParserImpl()
        val input = "/read: file.txt #L1-L10"
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        val commandBlock = children.find { it.type == "COMMAND_BLOCK" }
        assertNotNull(commandBlock, "Should contain a command block")
        
        if (commandBlock is DevInCommandBlockNode) {
            assertEquals("read", commandBlock.commandId)
            assertEquals("file.txt", commandBlock.commandProp)
            assertEquals("L1-L10", commandBlock.lineInfo)
        }
    }
    
    @Test
    fun testVariableBlockParsing() {
        val parser = DevInParserImpl()
        val input = "\$myVariable"
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        val variableBlock = children.find { it.type == "VARIABLE_BLOCK" }
        assertNotNull(variableBlock, "Should contain a variable block")
        
        if (variableBlock is DevInVariableBlockNode) {
            assertEquals("myVariable", variableBlock.variableId)
        }
    }
    
    @Test
    fun testCodeBlockParsing() {
        val parser = DevInParserImpl()
        val input = """
            ```javascript
            console.log("Hello World");
            ```
        """.trimIndent()
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        val codeBlock = children.find { it.type == "CODE_BLOCK" }
        assertNotNull(codeBlock, "Should contain a code block")
        
        if (codeBlock is DevInCodeBlockNode) {
            assertEquals("javascript", codeBlock.languageId)
            assertTrue(codeBlock.content.contains("console.log"))
        }
    }
    
    @Test
    fun testFrontMatterParsing() {
        val parser = DevInParserImpl()
        val input = """
            ---
            name: test
            version: 1.0
            enabled: true
            ---
            
            Content here
        """.trimIndent()
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        val frontMatter = children.find { it.type == "FRONT_MATTER_HEADER" }
        assertNotNull(frontMatter, "Should contain front matter")
        
        if (frontMatter is DevInFrontMatterNode) {
            assertTrue(frontMatter.entries.isNotEmpty(), "Should have front matter entries")
        }
    }
    
    @Test
    fun testExpressionBlockParsing() {
        val parser = DevInParserImpl()
        val input = "#if (condition == true)"
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        val expressionBlock = children.find { it.type == "EXPRESSION_BLOCK" }
        assertNotNull(expressionBlock, "Should contain an expression block")
    }
    
    @Test
    fun testComplexDocumentParsing() {
        val parser = DevInParserImpl()
        val input = """
            ---
            name: complex-example
            agent: myAgent
            version: 1.0
            ---
            
            # Complex DevIn Document
            
            Hello @myAgent, please /read: file.txt and process ${'$'}data.
            
            ```javascript
            console.log("Processing data");
            const result = process(data);
            ```
            
            #if (data.length > 0)
                Process the data
            #end
            
            // This is a comment
            [This is a content comment] Additional info
        """.trimIndent()
        
        val result = parser.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors: ${result.errors}")
        
        val children = result.ast!!.children
        assertTrue(children.isNotEmpty(), "Should have child nodes")
        
        // Check for different node types
        val nodeTypes = children.map { it.type }.toSet()
        println("Node types found: $nodeTypes")
        
        // Should contain various types of nodes
        assertTrue(nodeTypes.contains("FRONT_MATTER_HEADER"), "Should have front matter")
        assertTrue(nodeTypes.contains("AGENT_BLOCK") || nodeTypes.contains("TEXT_SEGMENT"), "Should have agent or text")
        assertTrue(nodeTypes.contains("CODE_BLOCK"), "Should have code block")
        
        println("Total child nodes: ${children.size}")
        children.forEachIndexed { index, node ->
            println("Node $index: type=${node.type}, line=${node.line}, col=${node.column}")
        }
    }
    
    @Test
    fun testParseRule() {
        val parser = DevInParserImpl()
        
        // Test parsing specific rules
        val agentResult = parser.parseRule("agentBlock", "@testAgent")
        assertNotNull(agentResult.ast, "Should parse agent block")
        assertEquals("AGENT_BLOCK", agentResult.ast!!.type)
        
        val commandResult = parser.parseRule("commandBlock", "/test: value")
        assertNotNull(commandResult.ast, "Should parse command block")
        assertEquals("COMMAND_BLOCK", commandResult.ast!!.type)
        
        val variableResult = parser.parseRule("variableBlock", "\$testVar")
        assertNotNull(variableResult.ast, "Should parse variable block")
        assertEquals("VARIABLE_BLOCK", variableResult.ast!!.type)
    }
    
    @Test
    fun testErrorHandling() {
        val parser = DevInParserImpl()
        
        // Test with invalid rule name
        val result = parser.parseRule("invalidRule", "test")
        assertNotNull(result.errors, "Should have errors")
        assertTrue(result.errors.isNotEmpty(), "Should report error for invalid rule")
    }
    
    @Test
    fun testTokenStreamParsing() {
        val lexer = DevInLexerImpl()
        val parser = DevInParserImpl()
        
        val input = "@agent /command \$variable"
        val tokens = lexer.tokenize(input)
        
        val result = parser.parseTokens(tokens)
        
        assertNotNull(result.ast, "Should produce an AST from tokens")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        
        val children = result.ast!!.children
        assertTrue(children.isNotEmpty(), "Should have child nodes")
    }
}
