package cc.unitmesh.indexer.naming

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test for CamelCaseSplitter
 */
class CamelCaseSplitterTest {
    
    @Test
    fun testSimpleCamelCase() {
        val result = CamelCaseSplitter.split("userController")
        assertEquals(listOf("user", "Controller"), result)
    }
    
    @Test
    fun testPascalCase() {
        val result = CamelCaseSplitter.split("UserController")
        assertEquals(listOf("User", "Controller"), result)
    }
    
    @Test
    fun testAcronyms() {
        val result = CamelCaseSplitter.split("XMLHttpRequest")
        assertEquals(listOf("XML", "Http", "Request"), result)
    }
    
    @Test
    fun testNumbersInName() {
        val result = CamelCaseSplitter.split("user2Controller")
        assertEquals(listOf("user", "2", "Controller"), result)
    }
    
    @Test
    fun testSingleWord() {
        val result = CamelCaseSplitter.split("user")
        assertEquals(listOf("user"), result)
    }
    
    @Test
    fun testEmptyString() {
        val result = CamelCaseSplitter.split("")
        assertEquals(emptyList(), result)
    }
    
    @Test
    fun testSplitAndFilter() {
        val result = CamelCaseSplitter.splitAndFilter("getUserById")
        // Should filter out common technical words like "get", "By", "Id"
        assertEquals(listOf("User"), result)
    }
    
    @Test
    fun testSplitAndFilterWithSuffixRules() {
        val suffixRules = CommonSuffixRules()
        val result = CamelCaseSplitter.splitAndFilter("UserController", suffixRules)
        // Should remove "Controller" suffix and return "User"
        assertEquals(listOf("User"), result)
    }
}

/**
 * Test for LanguageSuffixRules
 */
class LanguageSuffixRulesTest {
    
    @Test
    fun testCommonSuffixRules() {
        val rules = CommonSuffixRules()
        
        assertEquals("User", rules.normalize("UserController"))
        assertEquals("Blog", rules.normalize("BlogService"))
        assertEquals("Comment", rules.normalize("CommentDTO"))
        assertEquals("Payment", rules.normalize("PaymentEntity"))
        assertEquals("Order", rules.normalize("OrderUtils"))
    }
    
    @Test
    fun testNoSuffixToRemove() {
        val rules = CommonSuffixRules()
        
        assertEquals("User", rules.normalize("User"))
        assertEquals("Blog", rules.normalize("Blog"))
        assertEquals("CustomName", rules.normalize("CustomName"))
    }
    
    @Test
    fun testLongestSuffixFirst() {
        val rules = CommonSuffixRules()
        
        // "RestController" should be removed instead of just "Controller"
        assertEquals("User", rules.normalize("UserRestController"))
    }
}
