package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * DocQL 端到端集成测试
 * 测试从文档解析到查询执行的完整流程
 */
class DocQLIntegrationTest {
    
    /**
     * 模拟一个真实的 README.md 文档
     */
    private fun createReadmeDocument(): DocumentFile {
        return DocumentFile(
            name = "README.md",
            path = "/project/README.md",
            metadata = DocumentMetadata(
                lastModified = 1700000000000L,
                fileSize = 5000,
                language = "markdown",
                mimeType = "text/markdown",
                chapterCount = 5
            ),
            toc = listOf(
                // Level 1 - 项目标题
                TOCItem(
                    level = 1,
                    title = "AutoCRUD Project",
                    anchor = "#autocrud-project",
                    lineNumber = 1,
                    children = listOf(
                        // Level 2 - 子章节
                        TOCItem(
                            level = 2,
                            title = "Overview",
                            anchor = "#overview",
                            lineNumber = 5
                        ),
                        TOCItem(
                            level = 2,
                            title = "Features",
                            anchor = "#features",
                            lineNumber = 15,
                            children = listOf(
                                TOCItem(
                                    level = 3,
                                    title = "Core Features",
                                    anchor = "#core-features",
                                    lineNumber = 17
                                )
                            )
                        )
                    )
                ),
                // Level 1 - 安装
                TOCItem(
                    level = 1,
                    title = "Installation",
                    anchor = "#installation",
                    lineNumber = 30,
                    children = listOf(
                        TOCItem(
                            level = 2,
                            title = "Requirements",
                            anchor = "#requirements",
                            lineNumber = 32
                        ),
                        TOCItem(
                            level = 2,
                            title = "Quick Start",
                            anchor = "#quick-start",
                            lineNumber = 40
                        )
                    )
                ),
                // Level 1 - 架构
                TOCItem(
                    level = 1,
                    title = "Architecture",
                    anchor = "#architecture",
                    lineNumber = 50,
                    children = listOf(
                        TOCItem(
                            level = 2,
                            title = "System Design",
                            anchor = "#system-design",
                            lineNumber = 52
                        ),
                        TOCItem(
                            level = 2,
                            title = "Database Schema",
                            anchor = "#database-schema",
                            lineNumber = 60
                        )
                    )
                ),
                // Level 1 - API
                TOCItem(
                    level = 1,
                    title = "API Reference",
                    anchor = "#api-reference",
                    lineNumber = 70
                ),
                // Level 1 - 许可证
                TOCItem(
                    level = 1,
                    title = "License",
                    anchor = "#license",
                    lineNumber = 100
                )
            ),
            entities = listOf(
                Entity.Term(
                    name = "CRUD",
                    definition = "Create, Read, Update, Delete",
                    location = Location("#crud-term", line = 8)
                ),
                Entity.API(
                    name = "createUser",
                    signature = "createUser(user: User): Promise<User>",
                    location = Location("#create-user-api", line = 72)
                ),
                Entity.API(
                    name = "getUser",
                    signature = "getUser(id: string): Promise<User>",
                    location = Location("#get-user-api", line = 75)
                ),
                Entity.ClassEntity(
                    name = "UserService",
                    packageName = "com.example.service",
                    location = Location("#user-service-class", line = 80)
                ),
                Entity.FunctionEntity(
                    name = "validateUser",
                    signature = "validateUser(user: User): boolean",
                    location = Location("#validate-user-fn", line = 85)
                )
            )
        )
    }
    
    @Test
    fun `test query all level 1 headings from README`() = runTest {
        // Given: README.md 文档
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        // When: 查询所有一级标题
        val query = parseDocQL("$.toc[?(@.level==1)]")
        val result = executor.execute(query)
        
        // Then: 应该返回 5 个一级标题
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(5, result.items.size, "应该有 5 个一级标题")
        
        // 验证标题内容
        val titles = result.items.map { it.title }
        assertTrue(titles.contains("AutoCRUD Project"), "应包含 AutoCRUD Project")
        assertTrue(titles.contains("Installation"), "应包含 Installation")
        assertTrue(titles.contains("Architecture"), "应包含 Architecture")
        assertTrue(titles.contains("API Reference"), "应包含 API Reference")
        assertTrue(titles.contains("License"), "应包含 License")
        
        // 验证所有项的 level 都是 1
        assertTrue(result.items.all { it.level == 1 }, "所有结果的 level 都应该是 1")
    }
    
    @Test
    fun `test query level 2 headings from README`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.toc[?(@.level==2)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(6, result.items.size, "应该有 6 个二级标题")
        
        val titles = result.items.map { it.title }
        assertTrue(titles.contains("Overview"))
        assertTrue(titles.contains("Features"))
        assertTrue(titles.contains("Requirements"))
        assertTrue(titles.contains("Quick Start"))
        assertTrue(titles.contains("System Design"))
        assertTrue(titles.contains("Database Schema"))
    }
    
    @Test
    fun `test query headings containing Architecture`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("""$.toc[?(@.title~="Architecture")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertTrue(result.items.size >= 1, "应该找到包含 Architecture 的标题")
        assertTrue(result.items.any { it.title.contains("Architecture", ignoreCase = true) })
    }
    
    @Test
    fun `test query all TOC items`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.toc[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // 5 个一级 + 6 个二级 + 1 个三级 = 12 个（扁平化后）
        assertEquals(12, result.items.size, "扁平化后应该有 12 个 TOC 项")
    }
    
    @Test
    fun `test query first TOC item`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.toc[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(1, result.items.size)
        assertEquals("AutoCRUD Project", result.items[0].title)
        assertEquals(1, result.items[0].level)
    }
    
    @Test
    fun `test query all entities from README`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.entities[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(5, result.items.size, "应该有 5 个实体")
    }
    
    @Test
    fun `test query API entities`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="API")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(2, result.items.size, "应该有 2 个 API 实体")
        
        val apiNames = result.items.map { it.name }
        assertTrue(apiNames.contains("createUser"))
        assertTrue(apiNames.contains("getUser"))
    }
    
    @Test
    fun `test query Term entities`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("""$.entities[?(@.type=="Term")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        assertEquals(1, result.items.size)
        assertEquals("CRUD", result.items[0].name)
        assertIs<Entity.Term>(result.items[0])
    }
    
    @Test
    fun `test query entities by name contains User`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("""$.entities[?(@.name~="User")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Entities>(result)
        // createUser, getUser, UserService, validateUser = 4
        assertEquals(4, result.items.size, "应该找到 4 个包含 User 的实体")
    }
    
    @Test
    fun `test query level greater than 1`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.toc[?(@.level>1)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // 6 个二级 + 1 个三级 = 7
        assertEquals(7, result.items.size)
        assertTrue(result.items.all { it.level > 1 })
    }
    
    @Test
    fun `test query level less than 3`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("$.toc[?(@.level<3)]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        // 5 个一级 + 6 个二级 = 11
        assertEquals(11, result.items.size)
        assertTrue(result.items.all { it.level < 3 })
    }
    
    @Test
    fun `test complete workflow from parse to query to result`() = runTest {
        // Step 1: 创建文档（模拟从 README.md 解析）
        println("Step 1: 创建 README.md 文档")
        val readme = createReadmeDocument()
        println("  - 文档名称: ${readme.name}")
        println("  - TOC 项数: ${readme.toc.size} 个一级标题")
        println("  - 实体数量: ${readme.entities.size}")
        
        // Step 2: 解析 DocQL 查询
        println("\nStep 2: 解析 DocQL 查询")
        val queryString = "$.toc[?(@.level==1)]"
        println("  - 查询字符串: $queryString")
        
        val query = parseDocQL(queryString)
        println("  - 解析成功，节点数: ${query.nodes.size}")
        
        // Step 3: 执行查询
        println("\nStep 3: 执行查询")
        val executor = DocQLExecutor(readme, null)
        val result = executor.execute(query)
        
        // Step 4: 验证结果
        println("\nStep 4: 验证结果")
        assertIs<DocQLResult.TocItems>(result)
        println("  - 结果类型: TocItems")
        println("  - 找到 ${result.items.size} 个一级标题:")
        result.items.forEach { item ->
            println("    * ${item.title} (level=${item.level}, anchor=${item.anchor})")
        }
        
        assertEquals(5, result.items.size)
        println("\n✅ 端到端测试通过！")
    }
    
    @Test
    fun `test error handling for invalid query`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        // 测试无效查询
        val result = executeDocQL("$.invalid[*]", readme, null)
        
        assertIs<DocQLResult.Error>(result)
        assertTrue(result.message.contains("Unknown context"))
    }
    
    @Test
    fun `test empty result`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        // 查询一个不存在的标题
        val query = parseDocQL("""$.toc[?(@.title~="NonExistent")]""")
        val result = executor.execute(query)
        
        // Should return Empty for queries with no matches
        assertTrue(result is DocQLResult.Empty || 
            (result is DocQLResult.TocItems && result.items.isEmpty()),
            "不存在的标题应该返回空结果")
    }
    
    @Test
    fun `test query with exact title match`() = runTest {
        val readme = createReadmeDocument()
        val executor = DocQLExecutor(readme, null)
        
        val query = parseDocQL("""$.toc[?(@.title=="Architecture")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.TocItems>(result)
        assertEquals(1, result.items.size)
        assertEquals("Architecture", result.items[0].title)
    }
}

