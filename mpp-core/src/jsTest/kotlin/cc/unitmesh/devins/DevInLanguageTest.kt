package cc.unitmesh.devins

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DevInLanguageTest {
    
    @Test
    fun testBasicLanguageUsage() {
        val devIn = DevInLanguage.create()
        val input = "Hello @agent, please /read: file.txt"
        
        val result = devIn.parse(input)
        
        assertNotNull(result.ast, "Should produce an AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }
    
    @Test
    fun testTokenization() {
        val devIn = DevInLanguage.create()
        val input = "@agent /command \$variable #expression"
        
        val tokens = devIn.tokenize(input)
        
        assertTrue(tokens.isNotEmpty(), "Should produce tokens")
        
        val tokenTypes = tokens.map { it.type }
        println("Token types: $tokenTypes")
        
        // Should contain various token types
        assertTrue(tokenTypes.any { it.contains("agent", ignoreCase = true) })
        assertTrue(tokenTypes.any { it.contains("command", ignoreCase = true) })
        assertTrue(tokenTypes.any { it.contains("variable", ignoreCase = true) })
    }
    
    @Test
    fun testValidation() {
        val devIn = DevInLanguage.create()
        
        // Valid input
        val validInput = "@agent /command"
        assertTrue(devIn.isValid(validInput), "Should be valid")
        
        val errors = devIn.validate(validInput)
        assertTrue(errors.isEmpty(), "Should have no validation errors")
    }
    
    @Test
    fun testStaticMethods() {
        val input = "Hello @world"
        
        val parseResult = DevInLanguage.parse(input)
        assertNotNull(parseResult.ast, "Static parse should work")
        
        val tokens = DevInLanguage.tokenize(input)
        assertTrue(tokens.isNotEmpty(), "Static tokenize should work")
    }
    
    @Test
    fun testComplexDocument() {
        val devIn = DevInLanguage.create()
        val input = """
            ---
            name: test-document
            agent: myAgent
            version: 1.0
            enabled: true
            ---
            
            # Test Document
            
            Hello @myAgent, please analyze this code:
            
            ```javascript
            function hello() {
                console.log("Hello World");
                return "success";
            }
            ```
            
            Then /read: data.json and process ${'$'}result.
            
            #if (result.status == "success")
                Continue processing
            #else
                Handle error
            #end
            
            // Final comment
        """.trimIndent()
        
        val result = devIn.parse(input)
        
        assertNotNull(result.ast, "Should parse complex document")
        assertTrue(result.errors.isEmpty(), "Should have no errors: ${result.errors}")
        
        val children = result.ast!!.children
        assertTrue(children.isNotEmpty(), "Should have child nodes")
        
        println("Parsed ${children.size} top-level nodes")
        children.forEach { node ->
            println("- ${node.type} at line ${node.line}")
        }
    }
    
    @Test
    fun testFrontMatterExtraction() {
        val input = """
            ---
            name: test
            version: 1.0
            ---
            Content
        """.trimIndent()

        val devIn = DevInLanguage.create()
        val result = devIn.parse(input)

        assertNotNull(result.ast, "Should parse front matter")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        // TODO: Add front matter extraction from AST
    }

    @Test
    fun testFrontMatterExtractionWithoutFrontMatter() {
        val input = "Just regular content"

        val devIn = DevInLanguage.create()
        val result = devIn.parse(input)

        assertNotNull(result.ast, "Should parse content without front matter")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
    }
    
    @Test
    fun testCodeBlockExtraction() {
        val input = """
            ```javascript
            console.log("test");
            ```

            Some text

            ```python
            print("hello")
            ```
        """.trimIndent()

        val devIn = DevInLanguage.create()
        val result = devIn.parse(input)

        assertNotNull(result.ast, "Should parse code blocks")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        // TODO: Add code block extraction from AST
    }

    @Test
    fun testVariableExtraction() {
        val input = """
            Process ${'$'}data and ${'$'}config.
            Also use ${'$'}settings.
        """.trimIndent()

        val devIn = DevInLanguage.create()
        val result = devIn.parse(input)

        assertNotNull(result.ast, "Should parse variables")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        // TODO: Add variable extraction from AST
    }

    @Test
    fun testAgentExtraction() {
        val input = """
            Hello @agent1, please work with @agent2.
            Also notify @admin.
        """.trimIndent()

        val devIn = DevInLanguage.create()
        val result = devIn.parse(input)

        assertNotNull(result.ast, "Should parse agents")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        // TODO: Add agent extraction from AST
    }
    
    @Test
    fun testDevInProcessor() {
        val config = DevInConfig(
            enableFrontMatter = true,
            enableCodeBlocks = true,
            enableVariables = true,
            enableAgents = true,
            enableCommands = true,
            strictMode = false
        )
        
        val processor = DevInProcessor(config)
        val input = """
            ---
            name: test
            ---
            
            Hello @agent, /read: file.txt and process ${'$'}data.
            
            ```javascript
            console.log("test");
            ```
        """.trimIndent()
        
        val result = processor.process(input)
        
        assertNotNull(result.ast, "Should produce AST")
        assertTrue(result.errors.isEmpty(), "Should have no errors")
        assertNotNull(result.frontMatter, "Should extract front matter")
        assertNotNull(result.codeBlocks, "Should extract code blocks")
        assertNotNull(result.variables, "Should extract variables")
        assertNotNull(result.agents, "Should extract agents")
    }
    
    @Test
    fun testDevInProcessorWithDisabledFeatures() {
        val config = DevInConfig(
            enableFrontMatter = false,
            enableCodeBlocks = false,
            enableVariables = false,
            enableAgents = false,
            enableCommands = false,
            strictMode = true
        )
        
        val processor = DevInProcessor(config)
        val input = """
            ---
            name: test
            ---
            
            Hello @agent
        """.trimIndent()
        
        val result = processor.process(input)
        
        assertNotNull(result.ast, "Should produce AST")
        assertEquals(null, result.frontMatter, "Should not extract front matter when disabled")
        assertTrue(result.codeBlocks.isEmpty(), "Should not extract code blocks when disabled")
        assertTrue(result.variables.isEmpty(), "Should not extract variables when disabled")
        assertTrue(result.agents.isEmpty(), "Should not extract agents when disabled")
    }
    
    @Test
    fun testErrorReporting() {
        val devIn = DevInLanguage.create()
        
        // Test with potentially problematic input
        val input = "Unclosed front matter:\n---\nname: test\n"
        
        val result = devIn.parse(input)
        
        // Should still produce some result, even if there are issues
        assertNotNull(result, "Should always return a result")
        
        if (result.errors.isNotEmpty()) {
            println("Errors found (expected for this test):")
            result.errors.forEach { error ->
                println("- ${error.message} at line ${error.line}, column ${error.column}")
            }
        }
    }
    
    @Test
    fun testLineAndColumnTracking() {
        val devIn = DevInLanguage.create()
        val input = """
            Line 1
            Line 2 with @agent
            Line 3 with /command
        """.trimIndent()
        
        val tokens = devIn.tokenize(input)
        
        // Check that line numbers are tracked correctly
        val agentToken = tokens.find { it.value == "@" }
        if (agentToken != null) {
            assertEquals(2, agentToken.line, "Agent token should be on line 2")
        }
        
        val commandToken = tokens.find { it.value == "/" }
        if (commandToken != null) {
            assertEquals(3, commandToken.line, "Command token should be on line 3")
        }
    }
}
