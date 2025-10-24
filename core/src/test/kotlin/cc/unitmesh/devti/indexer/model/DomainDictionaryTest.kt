package cc.unitmesh.devti.indexer.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainDictionaryTest {
    
    @Test
    fun testGetAllNames() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1),
            SemanticName("Blog", ElementType.FILE, 1)
        )
        val level2 = listOf(
            SemanticName("createUser", ElementType.METHOD, 2),
            SemanticName("deleteBlog", ElementType.METHOD, 2)
        )
        val dict = DomainDictionary(level1, level2)
        
        val allNames = dict.getAllNames()
        assertEquals(4, allNames.size)
        assertTrue(allNames.contains("User"))
        assertTrue(allNames.contains("Blog"))
        assertTrue(allNames.contains("createUser"))
        assertTrue(allNames.contains("deleteBlog"))
    }
    
    @Test
    fun testGetAllNamesWithType() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1),
            SemanticName("createUser", ElementType.METHOD, 1)
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val namesWithType = dict.getAllNamesWithType()
        assertEquals(2, namesWithType.size)
        assertTrue(namesWithType[0].contains("FILE"))
        assertTrue(namesWithType[1].contains("METHOD"))
    }
    
    @Test
    fun testGetTotalTokens() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1),
            SemanticName("Blog", ElementType.FILE, 1)
        )
        val level2 = listOf(
            SemanticName("createUser", ElementType.METHOD, 2),
            SemanticName("deleteBlog", ElementType.METHOD, 2)
        )
        val dict = DomainDictionary(level1, level2)
        
        assertEquals(6, dict.getTotalTokens())
    }
    
    @Test
    fun testToSimpleList() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1),
            SemanticName("Blog", ElementType.FILE, 1)
        )
        val level2 = listOf(
            SemanticName("create", ElementType.METHOD, 1)
        )
        val dict = DomainDictionary(level1, level2)
        
        val result = dict.toSimpleList()
        assertEquals("User, Blog, create", result)
    }
    
    @Test
    fun testToCsvFormat() {
        val level1 = listOf(
            SemanticName(
                name = "User",
                type = ElementType.FILE,
                tokens = 1,
                source = "User.java",
                original = "User"
            )
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val csv = dict.toCsvFormat()
        assertTrue(csv.contains("名称,类型,来源,原始名称,Token数"))
        assertTrue(csv.contains("User,FILE,User.java,User,1"))
    }
    
    @Test
    fun testRemoveDuplicates() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1),
            SemanticName("User", ElementType.FILE, 1)  // Duplicate
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val allNames = dict.getAllNames()
        assertEquals(1, allNames.size)  // Should remove duplicates
    }
}
