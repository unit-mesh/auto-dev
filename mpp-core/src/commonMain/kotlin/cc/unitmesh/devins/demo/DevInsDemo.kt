package cc.unitmesh.devins.demo

import cc.unitmesh.devins.lexer.DevInsLexer
import cc.unitmesh.devins.parser.DevInsParser
import cc.unitmesh.devins.test.TestRunner
import cc.unitmesh.devins.test.SimpleParsingTest
import cc.unitmesh.devins.test.SimpleLexingTest

/**
 * DevIns 语言解析器演示
 */
object DevInsDemo {
    
    /**
     * 运行基本的词法分析演示
     */
    fun runLexerDemo() {
        println("=== DevIns Lexer Demo ===")
        
        val samples = listOf(
            "@ / $" to "Basic tokens",
            "when case default if else" to "Keywords",
            ": == != < > <= >= && ||" to "Operators",
            "( ) [ ] { }" to "Brackets",
            "identifier test_name" to "Identifiers",
            "123 456" to "Numbers",
            "\"hello world\"" to "Quoted strings",
            "---\nname: test\n---" to "Front matter",
            "```kotlin\ncode\n```" to "Code block",
            "// comment" to "Comments"
        )
        
        for ((input, description) in samples) {
            println("\n--- $description ---")
            println("Input: $input")
            
            try {
                val lexer = DevInsLexer(input)
                val tokens = lexer.tokenize()
                
                println("Tokens:")
                tokens.forEach { token ->
                    if (!token.isEof) {
                        println("  ${token.type}('${token.text}')")
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
    
    /**
     * 运行基本的语法分析演示
     */
    fun runParserDemo() {
        println("\n=== DevIns Parser Demo ===")
        
        val samples = listOf(
            "" to "Empty file",
            "Hello World" to "Simple text",
            "---\nname: \"test\"\n---" to "Simple front matter",
            "${'$'}variable" to "Variable",
            "/command" to "Command",
            "@agent" to "Agent"
        )
        
        for ((input, description) in samples) {
            println("\n--- $description ---")
            println("Input: $input")
            
            try {
                val parser = DevInsParser(input)
                val result = parser.parse()
                
                if (result.isSuccess) {
                    val ast = result.getOrThrow()
                    println("Parse successful!")
                    println("AST children count: ${ast.children.size}")
                    println("Node type: ${ast.nodeType}")
                } else {
                    val error = (result as cc.unitmesh.devins.parser.ParseResult.Failure).error
                    println("Parse failed: ${error.message}")
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }
    
    /**
     * 运行测试套件演示
     */
    fun runTestSuiteDemo() {
        println("\n=== DevIns Test Suite Demo ===")
        
        val testRunner = TestRunner()
        
        // 添加测试用例
        testRunner.addTestCase(SimpleLexingTest())
        testRunner.addTestCase(SimpleParsingTest())
        
        // 运行所有测试
        val result = testRunner.runAll()
        
        println("\n--- Test Results ---")
        println(result)
        
        if (!result.allPassed) {
            println("\nFailed tests:")
            result.results.filter { !it.passed }.forEach { testResult ->
                println("- ${testResult.testName}")
                testResult.results.filter { !it.passed }.forEach { singleResult ->
                    println("  * ${singleResult.name}: ${singleResult.message}")
                }
            }
        }
    }
    
    /**
     * 运行复杂文档解析演示
     */
    fun runComplexDocumentDemo() {
        println("\n=== Complex DevIns Document Demo ===")
        
        val complexDocument = """
            ---
            name: "Complex Example"
            variables:
              "sourceFiles": /.*\.kt${'$'}/ { find . -name "*.kt" }
              "testFiles": /.*Test\.kt${'$'}/ { find . -name "*Test.kt" }
            when: ${'$'}sourceFiles.length > 0
            onStreaming: { 
                log("Processing files...")
            }
            afterStreaming: {
                condition {
                    "success" { ${'$'}output.length > 0 }
                    "failure" { ${'$'}error != null }
                }
                case condition {
                    "success" { 
                        log("Processing completed successfully")
                    }
                    "failure" { 
                        log("Processing failed: " + ${'$'}error)
                    }
                    default { 
                        log("Unknown result")
                    }
                }
            }
            ---
            
            This is a complex DevIns document that demonstrates various features:
            
            1. **Variables**: We can reference source files like ${'$'}sourceFiles
            2. **Commands**: We can execute commands like /file:example.kt
            3. **Agents**: We can call agents like @helper
            
            ```kotlin
            // This is a code block
            fun example() {
                println("Hello from DevIns!")
            }
            ```
            
            The document can contain mixed content with variables (${'$'}testFiles), 
            commands (/analyze:${'$'}sourceFiles), and agents (@reviewer).
            
            ## Processing Results
            
            After processing, we can show results or handle errors appropriately.
        """.trimIndent()
        
        println("Input document:")
        println(complexDocument)
        println("\n--- Lexical Analysis ---")
        
        try {
            val lexer = DevInsLexer(complexDocument)
            val tokens = lexer.tokenize()
            
            println("Total tokens: ${tokens.size}")
            
            // 统计不同类型的 token
            val tokenCounts = tokens.groupBy { it.type }.mapValues { it.value.size }
            println("Token distribution:")
            tokenCounts.forEach { (type, count) ->
                if (count > 0) {
                    println("  $type: $count")
                }
            }
            
        } catch (e: Exception) {
            println("Lexer error: ${e.message}")
        }
        
        println("\n--- Syntax Analysis ---")
        
        try {
            val parser = DevInsParser(complexDocument)
            val result = parser.parse()
            
            if (result.isSuccess) {
                val ast = result.getOrThrow()
                println("Parse successful!")
                println("Root node: ${ast.nodeType}")
                println("Children count: ${ast.children.size}")
                
                // 检查前置元数据
                val frontMatter = ast.frontMatter
                if (frontMatter != null) {
                    println("Front matter entries: ${frontMatter.entries.size}")
                }
                
                // 检查代码块
                val codeBlocks = ast.codeBlocks
                println("Code blocks: ${codeBlocks.size}")
                
            } else {
                val error = (result as cc.unitmesh.devins.parser.ParseResult.Failure).error
                println("Parse failed: ${error.message} at ${error.position}")
            }
            
        } catch (e: Exception) {
            println("Parser error: ${e.message}")
        }
    }
    
    /**
     * 运行所有演示
     */
    fun runAllDemos() {
        runLexerDemo()
        runParserDemo()
        runTestSuiteDemo()
        runComplexDocumentDemo()
    }
}
