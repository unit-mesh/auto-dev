package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Test suite for DocQL code queries ($.code.*)
 * Tests the new TreeSitter-based code query functionality
 */
class DocQLCodeQueryTest {
    
    private val sampleKotlinCode = """
        package cc.unitmesh.devins.document.docql
        
        import cc.unitmesh.devins.document.*
        
        /**
         * DocQL Executor - executes DocQL queries against document data
         */
        class DocQLExecutor(
            private val documentFile: DocumentFile?,
            private val parserService: DocumentParserService?
        ) {
            
            /**
             * Execute a DocQL query
             */
            suspend fun execute(query: DocQLQuery): DocQLResult {
                return DocQLResult.Empty
            }
            
            private fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
                return DocQLResult.Empty
            }
            
            private fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
                return DocQLResult.Empty
            }
        }
        
        sealed class DocQLResult {
            data class TocItems(val items: List<String>) : DocQLResult()
            object Empty : DocQLResult()
        }
        
        class DocQLParser(private val tokens: List<DocQLToken>) {
            fun parse(): DocQLQuery {
                return DocQLQuery(emptyList())
            }
        }
    """.trimIndent()
    
    @Test
    fun `should query all classes using dollar code classes star`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.classes[*]
        val query = parseDocQL("$.code.classes[*]")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Entities)
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3) // DocQLExecutor, DocQLResult, DocQLParser
        println("✅ Found ${entities.totalCount} classes")
        
        entities.items.forEach { entity ->
            println("  📘 ${entity.name}")
        }
    }
    
    @Test
    fun `should query specific class using dollar code class function`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.class("DocQLExecutor")
        val query = parseDocQL("$.code.class(\"DocQLExecutor\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Chunks)
        val chunks = result as DocQLResult.Chunks
        assertTrue(chunks.totalCount >= 1)
        
        val chunk = chunks.items.first()
        println("✅ Found class: ${chunk.chapterTitle ?: "Unknown"}")
        println("Content preview: ${chunk.content.take(100)}...")
        
        assertTrue((chunk.chapterTitle ?: "").contains("DocQLExecutor"))
    }
    
    @Test
    fun `should query all functions using dollar code functions star`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.functions[*]
        val query = parseDocQL("$.code.functions[*]")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Entities)
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3) // execute, executeTocQuery, executeCodeQuery, parse
        println("✅ Found ${entities.totalCount} functions")
        
        entities.items.forEach { entity ->
            println("  ⚡ ${entity.name}")
        }
    }
    
    @Test
    fun `should query specific function using dollar code function`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.function("execute")
        val query = parseDocQL("$.code.function(\"execute\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Chunks)
        val chunks = result as DocQLResult.Chunks
        assertTrue(chunks.totalCount >= 1)
        
        println("✅ Found ${chunks.totalCount} execute functions")
        chunks.items.forEach { chunk ->
            println("  ⚡ ${chunk.chapterTitle}")
        }
    }
    
    @Test
    fun `should filter classes by name using contains`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.classes[?(@.name contains "Parser")]
        val query = parseDocQL("$.code.classes[?(@.name ~= \"Parser\")]")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Entities)
        val entities = result as DocQLResult.Entities
        println("✅ Found ${entities.totalCount} classes with 'Parser' in name")
        
        entities.items.forEach { entity ->
            assertTrue(entity.name.contains("Parser"))
            println("  📘 ${entity.name}")
        }
    }
    
    @Test
    fun `should use custom query to find code elements`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.query("execute")
        val query = parseDocQL("$.code.query(\"execute\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Chunks)
        val chunks = result as DocQLResult.Chunks
        assertTrue(chunks.totalCount >= 1)
        
        println("✅ Custom query found ${chunks.totalCount} matches")
    }
    
    @Test
    fun `should compare code query vs content query`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.class("DocQLExecutor")
        val codeQuery = parseDocQL("$.code.class(\"DocQLExecutor\")")
        val codeExecutor = DocQLExecutor(parsedFile, parser)
        val codeResult = codeExecutor.execute(codeQuery)
        
        // Execute $.content.heading("DocQLExecutor")
        val contentQuery = parseDocQL("$.content.heading(\"DocQLExecutor\")")
        val contentExecutor = DocQLExecutor(parsedFile, parser)
        val contentResult = contentExecutor.execute(contentQuery)
        
        println("📊 Comparison:")
        println("  $.code.class() result: ${if (codeResult is DocQLResult.Chunks) "Found ${codeResult.totalCount} chunks" else "No results"}")
        println("  $.content.heading() result: ${if (contentResult is DocQLResult.Chunks) "Found ${contentResult.totalCount} chunks" else "No results"}")
        
        // Both should work, but $.code.* is optimized for code
        assertTrue(codeResult is DocQLResult.Chunks || codeResult is DocQLResult.Empty)
        assertTrue(contentResult is DocQLResult.Chunks || contentResult is DocQLResult.Empty)
    }
    
    @Test
    fun `should query methods with method alias`() = runBlocking {
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.methods[*] (should be same as $.code.functions[*])
        val query = parseDocQL("$.code.methods[*]")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(result is DocQLResult.Entities)
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3)
        println("✅ Found ${entities.totalCount} methods (using 'methods' alias)")
    }
}

