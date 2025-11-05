package cc.unitmesh.indexer.template

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for TemplateEngine
 */
class TemplateEngineTest {
    
    private val engine = TemplateEngine()
    
    @Test
    fun testSimpleVariableReplacement() {
        val template = "Hello \$name, welcome to \$project!"
        val context = mapOf(
            "name" to "Alice",
            "project" to "AutoDev"
        )
        
        val result = engine.render(template, context)
        assertEquals("Hello Alice, welcome to AutoDev!", result)
    }
    
    @Test
    fun testBraceVariableReplacement() {
        val template = "Hello \${user.name}, your score is \${user.score}"
        val context = mapOf(
            "user" to mapOf(
                "name" to "Bob",
                "score" to 95
            )
        )
        
        val result = engine.render(template, context)
        assertEquals("Hello Bob, your score is 95", result)
    }
    
    @Test
    fun testTemplateContextObject() {
        val template = "Code: \${context.code}, README: \${context.readme}"
        val contextObj = DomainDictTemplateContext(
            code = "User, Blog, Comment",
            readme = "This is a blog application"
        )
        val context = mapOf("context" to contextObj)
        
        val result = engine.render(template, context)
        assertEquals("Code: User, Blog, Comment, README: This is a blog application", result)
    }
    
    @Test
    fun testMissingVariables() {
        val template = "Hello \$name, welcome to \$missing!"
        val context = mapOf("name" to "Alice")
        
        val result = engine.render(template, context)
        assertEquals("Hello Alice, welcome to \$missing!", result)
    }
    
    @Test
    fun testTemplateManager() {
        val manager = TemplateManager()
        
        // Test caching
        manager.cacheTemplate("test", "Hello \$name!")
        val result = manager.renderTemplate("test", mapOf("name" to "World"))
        assertEquals("Hello World!", result)
    }
    
    @Test
    fun testDefaultIndexerTemplate() {
        val manager = TemplateManager()
        val template = manager.getTemplate("indexer.vm")
        
        // Should contain key template elements
        assertTrue(template.contains("DDD"))
        assertTrue(template.contains("业务"))
        assertTrue(template.contains("\$context.code"))
        assertTrue(template.contains("\$context.readme"))
    }
    
    @Test
    fun testDefaultEnglishTemplate() {
        val manager = TemplateManager()
        val template = manager.getTemplate("indexer_en.vm")
        
        // Should contain key template elements
        assertTrue(template.contains("DDD"))
        assertTrue(template.contains("business"))
        assertTrue(template.contains("\$context.code"))
        assertTrue(template.contains("\$context.readme"))
    }
}
