package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DocQLExecutorTest {
    
    private fun createTestDocument(): DocumentFile {
        return DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 1000,
                language = "markdown"
            ),
            toc = listOf(
                TOCItem(
                    level = 1,
                    title = "Introduction",
                    anchor = "#intro",
                    children = listOf(
                        TOCItem(
                            level = 2,
                            title = "Overview",
                            anchor = "#overview"
                        )
                    )
                ),
                TOCItem(
                    level = 1,
                    title = "Architecture",
                    anchor = "#arch",
                    children = listOf(
                        TOCItem(
                            level = 2,
                            title = "System Design",
                            anchor = "#design"
                        ),
                        TOCItem(
                            level = 2,
                            title = "Database Design",
                            anchor = "#db-design"
                        )
                    )
                )
            ),
            entities = listOf(
                Entity.Term(
                    name = "API",
                    definition = "Application Programming Interface",
                    location = Location("#api")
                ),
                Entity.API(
                    name = "getUserById",
                    signature = "getUserById(id: String): User",
                    location = Location("#get-user")
                ),
                Entity.ClassEntity(
                    name = "UserService",
                    packageName = "com.example.service",
                    location = Location("#user-service")
                ),
                Entity.FunctionEntity(
                    name = "validateUser",
                    signature = "validateUser(user: User): Boolean",
                    location = Location("#validate")
                )
            )
        )
    }
    
    @Test
    fun `test execute TOC all query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.toc[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // Should return 5 items (flattened: 2 level-1 + 3 level-2)
        assertEquals(5, result.items.size)
    }
    
    @Test
    fun `test execute TOC index query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.toc[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(1, result.items.size)
        assertEquals("Introduction", result.items[0].title)
    }
    
    @Test
    fun `test execute TOC filter by level`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.toc[?(@.level==1)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(2, result.items.size)
        assertEquals("Introduction", result.items[0].title)
        assertEquals("Architecture", result.items[1].title)
    }
    
    @Test
    fun `test execute TOC filter by title contains`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.toc[?(@.title~="Design")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(2, result.items.size)
        assertEquals("System Design", result.items[0].title)
        assertEquals("Database Design", result.items[1].title)
    }
    
    @Test
    fun `test execute entities all query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.entities[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(4, result.items.size)
    }
    
    @Test
    fun `test execute entities index query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.entities[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertEquals("API", result.items[0].name)
    }
    
    @Test
    fun `test execute entities filter by type`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="API")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertIs<Entity.API>(result.items[0])
    }
    
    @Test
    fun `test execute entities filter by name`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.name=="UserService")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertIs<Entity.ClassEntity>(result.items[0])
    }
    
    @Test
    fun `test execute entities filter by name contains`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.name~="User")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        // Should match getUserById, UserService, validateUser
        assertEquals(3, result.items.size)
    }
    
    @Test
    fun `test execute heading level query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.content.h1("Introduction")""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(1, result.items.size)
        assertEquals("Introduction", result.items[0].title)
    }
    
    @Test
    fun `test execute h2 query with partial match`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.content.h2("Design")""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // Should match "System Design" and "Database Design"
        assertEquals(2, result.items.size)
    }
    
    @Test
    fun `test execute query with no document`() = runTest {
        val executor = DocQLExecutor(null, null)
        
        val query = parseDocQL("$.toc[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Error>(result)
        assert(result.message.contains("No document"))
    }
    
    @Test
    fun `test execute empty query`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = DocQLQuery(emptyList())
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Error>(result)
    }
    
    @Test
    fun `test execute invalid context`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.invalid[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Error>(result)
        assert(result.message.contains("Unknown context"))
    }
    
    @Test
    fun `test executeDocQL convenience function`() = runTest {
        val doc = createTestDocument()
        
        val result = executeDocQL("$.toc[*]", doc, null)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(5, result.items.size)
    }
    
    @Test
    fun `test executeDocQL with parse error`() = runTest {
        val doc = createTestDocument()
        
        val result = executeDocQL("invalid query", doc, null)
        
        assertIs<DocQLResult.Error>(result)
    }
    
    @Test
    fun `test TOC filter with greater than`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.toc[?(@.level>1)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // Should return level 2 items only
        assertEquals(3, result.items.size)
        assert(result.items.all { it.level == 2 })
    }
    
    @Test
    fun `test TOC filter with less than`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("$.toc[?(@.level<2)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // Should return level 1 items only
        assertEquals(2, result.items.size)
        assert(result.items.all { it.level == 1 })
    }
    
    @Test
    fun `test entities filter Term type`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="Term")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertIs<Entity.Term>(result.items[0])
        assertEquals("API", result.items[0].name)
    }
    
    @Test
    fun `test entities filter ClassEntity type`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="ClassEntity")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertIs<Entity.ClassEntity>(result.items[0])
    }
    
    @Test
    fun `test entities filter FunctionEntity type`() = runTest {
        val doc = createTestDocument()
        val executor = DocQLExecutor(doc, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="FunctionEntity")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertIs<Entity.FunctionEntity>(result.items[0])
    }
}

