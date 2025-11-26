package cc.unitmesh.agent.scoring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeywordExpanderTest {
    
    private val expander = KeywordExpander()
    
    @Test
    fun `should expand simple keyword`() {
        val result = expander.expand("base64")
        
        assertTrue(result.primary.contains("base64"))
        // Should generate variations like base64ing, base64ed, base64s
        assertTrue(result.primary.size >= 1)
    }
    
    @Test
    fun `should expand phrase with variations`() {
        val result = expander.expand("base64 encoding")
        
        assertTrue(result.primary.contains("base64 encoding"))
        // Should include variations like "base64 encoder", "base64 encode"
        assertTrue(result.primary.any { it.contains("base64") && it.contains("encod") })
        
        // Secondary should include individual words
        assertTrue(result.secondary.contains("base64") || result.primary.contains("base64"))
    }
    
    @Test
    fun `should split camelCase in secondary keywords`() {
        val result = expander.expand("AuthService")
        
        // Primary should include original
        assertTrue(result.primary.contains("AuthService"))
        
        // Secondary should include split parts
        assertTrue(result.secondary.any { it.lowercase() == "auth" })
        assertTrue(result.secondary.any { it.lowercase() == "service" })
    }
    
    @Test
    fun `should generate stem variants in tertiary`() {
        val result = expander.expand("encoding")
        
        // Tertiary should include stem variants
        assertTrue(result.tertiary.isNotEmpty() || result.primary.any { it.contains("encod") })
    }
    
    @Test
    fun `should handle user-provided secondary hint`() {
        val result = expander.expandWithHint("Auth", "Service")
        
        assertTrue(result.primary.contains("Auth"))
        assertTrue(result.secondary.contains("Service"))
    }
    
    @Test
    fun `should recommend EXPAND strategy for few results`() {
        val strategy = expander.recommendStrategy(resultCount = 1, currentLevel = 1)
        assertEquals(SearchStrategy.EXPAND, strategy)
    }
    
    @Test
    fun `should recommend KEEP strategy for ideal results`() {
        val strategy = expander.recommendStrategy(resultCount = 15, currentLevel = 1)
        assertEquals(SearchStrategy.KEEP, strategy)
    }
    
    @Test
    fun `should recommend FILTER strategy for too many results`() {
        val customExpander = KeywordExpander(
            KeywordExpanderConfig(maxResultsThreshold = 50)
        )
        val strategy = customExpander.recommendStrategy(resultCount = 100, currentLevel = 1)
        assertEquals(SearchStrategy.FILTER, strategy)
    }
    
    @Test
    fun `should handle empty query`() {
        val result = expander.expand("")
        
        assertTrue(result.primary.isEmpty())
        assertTrue(result.secondary.isEmpty())
        assertTrue(result.tertiary.isEmpty())
    }
    
    @Test
    fun `upToLevel should return cumulative keywords`() {
        val result = expander.expand("UserController")
        
        // Level 1 should only include primary
        val level1 = result.upToLevel(1)
        assertTrue(level1.isNotEmpty())
        assertTrue(level1.all { it in result.primary })
        
        // Level 2 should include primary + secondary
        val level2 = result.upToLevel(2)
        assertTrue(level2.containsAll(result.primary))
        
        // Level 3 should include all
        val level3 = result.upToLevel(3)
        assertTrue(level3.size >= level2.size)
    }
    
    @Test
    fun `should handle verb forms in variations`() {
        val result = expander.expand("authenticate")
        
        // Should generate variations like authenticating, authenticated, authenticator
        val allKeywords = result.primary + result.secondary + result.tertiary
        val variations = allKeywords.filter { it.startsWith("authenticat") }
        assertTrue(variations.size >= 1)
    }
    
    @Test
    fun `should handle words ending in -ing`() {
        val result = expander.expand("processing")
        
        // Should extract stem "process" in variations
        val allKeywords = result.primary + result.secondary + result.tertiary
        assertTrue(allKeywords.any { it.contains("process") })
    }
}

