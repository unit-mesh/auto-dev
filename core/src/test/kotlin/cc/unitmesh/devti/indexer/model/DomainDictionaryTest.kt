package cc.unitmesh.devti.indexer.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainDictionaryTest {
    
    @Test
    fun testGetAllNames() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1, weight = 0.7f),
            SemanticName("Blog", ElementType.FILE, 1, weight = 0.6f)
        )
        val level2 = listOf(
            SemanticName("createUser", ElementType.METHOD, 2, parentClassName = "User", weight = 0.7f),
            SemanticName("deleteBlog", ElementType.METHOD, 2, parentClassName = "Blog", weight = 0.6f)
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
            SemanticName("User", ElementType.FILE, 1, weight = 0.7f),
            SemanticName("Blog", ElementType.FILE, 1, weight = 0.6f)
        )
        val level2 = listOf(
            SemanticName("createUser", ElementType.METHOD, 2, weight = 0.7f),
            SemanticName("deleteBlog", ElementType.METHOD, 2, weight = 0.6f)
        )
        val dict = DomainDictionary(level1, level2)
        
        assertEquals(6, dict.getTotalTokens())
    }
    
    @Test
    fun testToSimpleList() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1, weight = 0.7f),
            SemanticName("Blog", ElementType.FILE, 1, weight = 0.6f)
        )
        val level2 = listOf(
            SemanticName("create", ElementType.METHOD, 1, weight = 0.7f)
        )
        val dict = DomainDictionary(level1, level2)
        
        val result = dict.toSimpleList()
        assertTrue(result.contains("User"))
        assertTrue(result.contains("Blog"))
        assertTrue(result.contains("create"))
    }
    
    @Test
    fun testToWeightedList() {
        // High weight items should appear first
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1, weight = 0.9f),
            SemanticName("Blog", ElementType.FILE, 1, weight = 0.3f)
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val result = dict.toWeightedList()
        val items = result.split(", ")
        assertEquals("User", items[0])  // High weight first
        assertEquals("Blog", items[1])  // Low weight second
    }
    
    @Test
    fun testToCsvFormat() {
        val level1 = listOf(
            SemanticName(
                name = "User",
                type = ElementType.FILE,
                tokens = 1,
                source = "User.java",
                original = "User",
                weight = 0.8f,
                packageName = "com.example.user",
                weightCategory = "High"
            )
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val csv = dict.toCsvFormat()
        assertTrue(csv.contains("名称,类型,来源,原始名称,Token数,权重,权重等级,所属包"))
        assertTrue(csv.contains("User"))
        assertTrue(csv.contains("High"))
        assertTrue(csv.contains("com.example.user"))
    }
    
    @Test
    fun testRemoveDuplicates() {
        val level1 = listOf(
            SemanticName("User", ElementType.FILE, 1, weight = 0.7f),
            SemanticName("User", ElementType.FILE, 1, weight = 0.7f)  // Duplicate
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val allNames = dict.getAllNames()
        assertEquals(1, allNames.size)  // Should remove duplicates
    }
    
    @Test
    fun testGetWeightStatistics() {
        val level1 = listOf(
            SemanticName("Critical", ElementType.FILE, 1, weight = 0.9f, weightCategory = "Critical"),
            SemanticName("High", ElementType.FILE, 1, weight = 0.7f, weightCategory = "High"),
            SemanticName("Medium", ElementType.FILE, 1, weight = 0.5f, weightCategory = "Medium"),
            SemanticName("Low", ElementType.FILE, 1, weight = 0.2f, weightCategory = "Low")
        )
        val dict = DomainDictionary(level1, emptyList())
        
        val stats = dict.getWeightStatistics()
        assertEquals(1, stats["criticalCount"])
        assertEquals(1, stats["highCount"])
        assertEquals(1, stats["mediumCount"])
        assertEquals(1, stats["lowCount"])
    }
}
