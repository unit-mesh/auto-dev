package cc.unitmesh.diagram

import cc.unitmesh.diagram.diagram.DiagramSketchProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GraphvizSketchProviderTest {
    
    private val provider = DiagramSketchProvider()
    
    @Test
    fun testSupportedLanguages() {
        // Test supported languages
        assertTrue(provider.isSupported("dot"))
        assertTrue(provider.isSupported("DOT"))
        assertTrue(provider.isSupported("graphviz"))
        assertTrue(provider.isSupported("GRAPHVIZ"))
        assertTrue(provider.isSupported("gv"))
        assertTrue(provider.isSupported("GV"))
        
        // Test unsupported languages
        assertFalse(provider.isSupported("java"))
        assertFalse(provider.isSupported("python"))
        assertFalse(provider.isSupported("mermaid"))
        assertFalse(provider.isSupported("plantuml"))
    }
    
    @Test
    fun testExtensionName() {
        // We can't easily test the create method without a full IntelliJ environment
        // But we can test that the provider correctly identifies supported languages
        val supportedLanguages = listOf("dot", "graphviz", "gv")
        
        supportedLanguages.forEach { lang ->
            assertTrue(provider.isSupported(lang), "Should support language: $lang")
        }
    }
    
    @Test
    fun testCaseInsensitive() {
        // Test that language detection is case-insensitive
        val testCases = listOf(
            "dot" to true,
            "DOT" to true,
            "Dot" to true,
            "dOt" to true,
            "graphviz" to true,
            "GRAPHVIZ" to true,
            "GraphViz" to true,
            "gv" to true,
            "GV" to true,
            "Gv" to true
        )
        
        testCases.forEach { (lang, expected) ->
            assertEquals(expected, provider.isSupported(lang), "Language '$lang' should be $expected")
        }
    }
}
