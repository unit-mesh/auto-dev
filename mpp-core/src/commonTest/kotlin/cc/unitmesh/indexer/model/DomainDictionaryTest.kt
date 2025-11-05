package cc.unitmesh.indexer.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test for DomainDictionary and related models
 */
class DomainDictionaryTest {
    
    private fun createTestDictionary(): DomainDictionary {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1, "UserController.java", "UserController", 0.8f, weightCategory = "Critical"),
            SemanticName("Blog", ElementType.FILE, 1, "BlogService.java", "BlogService", 0.7f, weightCategory = "High"),
            SemanticName("Comment", ElementType.FILE, 1, "CommentRepository.java", "CommentRepository", 0.6f, weightCategory = "Medium")
        )
        
        val level2 = listOf(
            SemanticName("getUser", ElementType.METHOD, 2, "UserController.getUser", "getUserById", 0.5f, parentClassName = "UserController", weightCategory = "Medium"),
            SemanticName("createBlog", ElementType.METHOD, 2, "BlogService.createBlog", "createBlog", 0.6f, parentClassName = "BlogService", weightCategory = "Medium"),
            SemanticName("findComment", ElementType.METHOD, 2, "CommentRepository.findComment", "findCommentByBlogId", 0.4f, parentClassName = "CommentRepository", weightCategory = "Low")
        )
        
        return DomainDictionary(level1, level2)
    }
    
    @Test
    fun testGetAllNames() {
        val dictionary = createTestDictionary()
        val allNames = dictionary.getAllNames()
        
        assertEquals(6, allNames.size)
        assertTrue(allNames.contains("User"))
        assertTrue(allNames.contains("Blog"))
        assertTrue(allNames.contains("Comment"))
        assertTrue(allNames.contains("getUser"))
        assertTrue(allNames.contains("createBlog"))
        assertTrue(allNames.contains("findComment"))
    }
    
    @Test
    fun testGetAllNamesSortedByWeight() {
        val dictionary = createTestDictionary()
        val sortedNames = dictionary.getAllNamesSortedByWeight()
        
        // Should be sorted by weight (highest first)
        assertEquals("User", sortedNames[0]) // weight 0.8
        assertEquals("Blog", sortedNames[1]) // weight 0.7
        assertEquals("Comment", sortedNames[2]) // weight 0.6
        assertEquals("createBlog", sortedNames[3]) // weight 0.6
        assertEquals("getUser", sortedNames[4]) // weight 0.5
        assertEquals("findComment", sortedNames[5]) // weight 0.4
    }
    
    @Test
    fun testGetTotalTokens() {
        val dictionary = createTestDictionary()
        val totalTokens = dictionary.getTotalTokens()
        
        // 3 level1 items (1 token each) + 3 level2 items (2 tokens each) = 9 tokens
        assertEquals(9, totalTokens)
    }
    
    @Test
    fun testToCsvFormat() {
        val dictionary = createTestDictionary()
        val csv = dictionary.toCsvFormat()
        
        assertTrue(csv.startsWith("名称,类型,来源,原始名称,Token数,权重,权重等级,所属包"))
        assertTrue(csv.contains("User,FILE,UserController.java,UserController,1,80%,Critical,"))
        assertTrue(csv.contains("Blog,FILE,BlogService.java,BlogService,1,70%,High,"))
    }
    
    @Test
    fun testToSimpleList() {
        val dictionary = createTestDictionary()
        val simpleList = dictionary.toSimpleList()
        
        assertEquals("User, Blog, Comment, getUser, createBlog, findComment", simpleList)
    }
    
    @Test
    fun testToWeightedList() {
        val dictionary = createTestDictionary()
        val weightedList = dictionary.toWeightedList()
        
        // Should be ordered by weight
        assertEquals("User, Blog, Comment, createBlog, getUser, findComment", weightedList)
    }
    
    @Test
    fun testGetWeightStatistics() {
        val dictionary = createTestDictionary()
        val stats = dictionary.getWeightStatistics()
        
        assertEquals(0.6f, stats["averageWeight"] as Float, 0.01f)
        assertEquals(0.8f, stats["maxWeight"] as Float)
        assertEquals(0.4f, stats["minWeight"] as Float)
        assertEquals(1, stats["criticalCount"] as Int) // User with 0.8
        assertEquals(1, stats["highCount"] as Int) // Blog with 0.7
        assertEquals(3, stats["mediumCount"] as Int) // Comment, createBlog, getUser
        assertEquals(1, stats["lowCount"] as Int) // findComment with 0.4
    }
    
    @Test
    fun testGroupMethodsByClass() {
        val dictionary = createTestDictionary()
        val groups = dictionary.groupMethodsByClass()
        
        assertEquals(3, groups.size)
        
        val userGroup = groups.find { it.className == "UserController" }
        assertEquals(listOf("getUser"), userGroup?.methods)
        
        val blogGroup = groups.find { it.className == "BlogService" }
        assertEquals(listOf("createBlog"), blogGroup?.methods)
        
        val commentGroup = groups.find { it.className == "CommentRepository" }
        assertEquals(listOf("findComment"), commentGroup?.methods)
    }
}
