package cc.unitmesh.devti.vcs.context

import junit.framework.TestCase.assertTrue
import org.junit.Test

class TokenCounterTest {

    @Test
    fun testCountTokensSimpleText() {
        val counter = TokenCounter.DEFAULT
        
        val text = "Hello, world!"
        val tokens = counter.countTokens(text)
        
        // Should have some tokens
        assertTrue("Token count should be positive", tokens > 0)
        // Rough estimate: should be around 3-4 tokens
        assertTrue("Token count should be reasonable", tokens in 1..10)
    }

    @Test
    fun testCountTokensLongText() {
        val counter = TokenCounter.DEFAULT
        
        val text = """
            This is a longer text that contains multiple sentences.
            It should have more tokens than a simple greeting.
            We're testing the token counting functionality.
        """.trimIndent()
        
        val tokens = counter.countTokens(text)
        
        // Should have more tokens for longer text
        assertTrue("Token count should be positive", tokens > 0)
        assertTrue("Token count should be reasonable for long text", tokens > 10)
    }

    @Test
    fun testCountTokensMultipleStrings() {
        val counter = TokenCounter.DEFAULT
        
        val texts = listOf(
            "First string",
            "Second string",
            "Third string"
        )
        
        val totalTokens = counter.countTokens(texts)
        
        assertTrue("Total token count should be positive", totalTokens > 0)
    }

    @Test
    fun testCountTokensEmptyString() {
        val counter = TokenCounter.DEFAULT
        
        val tokens = counter.countTokens("")
        
        // Empty string should have 0 tokens
        assertTrue("Empty string should have 0 tokens", tokens == 0)
    }

    @Test
    fun testCountTokensCodeSnippet() {
        val counter = TokenCounter.DEFAULT
        
        val code = """
            fun main() {
                println("Hello, world!")
            }
        """.trimIndent()
        
        val tokens = counter.countTokens(code)
        
        assertTrue("Code snippet should have tokens", tokens > 0)
    }
}

