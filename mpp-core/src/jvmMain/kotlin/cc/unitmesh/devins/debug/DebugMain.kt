package cc.unitmesh.devins.debug

import cc.unitmesh.devins.lexer.DevInsLexer

fun main() {
    println("=== DevIns Lexer Debug ===")
    
    val testCases = listOf(
        "This is a text segment"
    )
    
    for (input in testCases) {
        println("\n--- Testing: '$input' ---")
        
        try {
            val lexer = DevInsLexer(input)
            val tokens = lexer.tokenize()
            
            println("Tokens (${tokens.size}):")
            tokens.forEach { token ->
                println("  ${token.type}('${token.text}')")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
