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
    
    // ====== Wildcard Support Tests ======
    
    @Test
    fun `should return all classes when using dollar code class with wildcard star`() = runBlocking {
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
        
        // Execute $.code.class("*") - should return all classes like $.code.classes[*]
        val query = parseDocQL("$.code.class(\"*\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        // Should return Entities result (same as $.code.classes[*])
        assertTrue(result is DocQLResult.Entities, "Expected Entities result but got: $result")
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3, "Expected at least 3 classes, got ${entities.totalCount}")
        println("✅ $.code.class(\"*\") found ${entities.totalCount} classes")
        
        entities.items.forEach { entity ->
            println("  📘 ${entity.name}")
        }
    }
    
    @Test
    fun `should return all functions when using dollar code function with wildcard star`() = runBlocking {
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
        
        // Execute $.code.function("*") - should return all functions like $.code.functions[*]
        val query = parseDocQL("$.code.function(\"*\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        // Should return Entities result (same as $.code.functions[*])
        assertTrue(result is DocQLResult.Entities, "Expected Entities result but got: $result")
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3, "Expected at least 3 functions, got ${entities.totalCount}")
        println("✅ $.code.function(\"*\") found ${entities.totalCount} functions")
        
        entities.items.forEach { entity ->
            println("  ⚡ ${entity.name}")
        }
    }
    
    @Test
    fun `should return all methods when using dollar code method with wildcard star`() = runBlocking {
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
        
        // Execute $.code.method("*") - should return all methods like $.code.methods[*]
        val query = parseDocQL("$.code.method(\"*\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        // Should return Entities result (same as $.code.methods[*])
        assertTrue(result is DocQLResult.Entities, "Expected Entities result but got: $result")
        val entities = result as DocQLResult.Entities
        assertTrue(entities.totalCount >= 3, "Expected at least 3 methods, got ${entities.totalCount}")
        println("✅ $.code.method(\"*\") found ${entities.totalCount} methods")
    }
    
    @Test
    fun `should return all chunks when using dollar code query with wildcard star`() = runBlocking {
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
        
        // Execute $.code.query("*") - should return all code chunks
        val query = parseDocQL("$.code.query(\"*\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        // Should return Chunks result or Empty (if no TOC items)
        assertTrue(
            result is DocQLResult.Chunks || result is DocQLResult.Empty,
            "Expected Chunks or Empty result but got: $result"
        )
        
        if (result is DocQLResult.Chunks) {
            println("✅ $.code.query(\"*\") found ${result.totalCount} chunks")
        } else {
            println("✅ $.code.query(\"*\") returned Empty (expected for code without TOC)")
        }
    }
    
    @Test
    fun `wildcard class query should have same result as classes array all`() = runBlocking {
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
        
        // Execute both queries
        val wildcardQuery = parseDocQL("$.code.class(\"*\")")
        val arrayQuery = parseDocQL("$.code.classes[*]")
        
        val executor = DocQLExecutor(parsedFile, parser)
        val wildcardResult = executor.execute(wildcardQuery)
        val arrayResult = executor.execute(arrayQuery)
        
        // Both should be Entities results with same count
        assertTrue(wildcardResult is DocQLResult.Entities)
        assertTrue(arrayResult is DocQLResult.Entities)
        
        val wildcardEntities = wildcardResult as DocQLResult.Entities
        val arrayEntities = arrayResult as DocQLResult.Entities
        
        assertTrue(
            wildcardEntities.totalCount == arrayEntities.totalCount,
            "Expected same count: wildcard=${wildcardEntities.totalCount}, array=${arrayEntities.totalCount}"
        )
        println("✅ $.code.class(\"*\") and $.code.classes[*] return same ${arrayEntities.totalCount} classes")
    }
    
    @Test
    fun `wildcard function query should have same result as functions array all`() = runBlocking {
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
        
        // Execute both queries
        val wildcardQuery = parseDocQL("$.code.function(\"*\")")
        val arrayQuery = parseDocQL("$.code.functions[*]")
        
        val executor = DocQLExecutor(parsedFile, parser)
        val wildcardResult = executor.execute(wildcardQuery)
        val arrayResult = executor.execute(arrayQuery)
        
        // Both should be Entities results with same count
        assertTrue(wildcardResult is DocQLResult.Entities)
        assertTrue(arrayResult is DocQLResult.Entities)
        
        val wildcardEntities = wildcardResult as DocQLResult.Entities
        val arrayEntities = arrayResult as DocQLResult.Entities
        
        assertTrue(
            wildcardEntities.totalCount == arrayEntities.totalCount,
            "Expected same count: wildcard=${wildcardEntities.totalCount}, array=${arrayEntities.totalCount}"
        )
        println("✅ $.code.function(\"*\") and $.code.functions[*] return same ${arrayEntities.totalCount} functions")
    }
    
    // ====== File Type Filtering Tests ======
    
    @Test
    fun `code query should return empty for markdown files`() = runBlocking {
        val markdownContent = """
            # Introduction
            
            This document describes how to parse data.
            
            ## Parse Methods
            
            The `parse()` function is used to analyze input.
            
            ```kotlin
            fun parse(input: String): Result {
                return Result.success()
            }
            ```
        """.trimIndent()
        
        val parser = MarkdownDocumentParser()
        val file = DocumentFile(
            name = "readme.md",
            path = "docs/readme.md",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = markdownContent.length.toLong(),
                formatType = DocumentFormatType.MARKDOWN  // This is a markdown file, not source code
            )
        )
        
        val parsedFile = parser.parse(file, markdownContent) as DocumentFile
        
        // Execute $.code.function("parse") - should return Empty for markdown files
        val query = parseDocQL("$.code.function(\"parse\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        // Should return Empty, not markdown heading results
        assertTrue(
            result is DocQLResult.Empty,
            "$.code.* queries should return Empty for markdown files, but got: $result"
        )
        println("✅ $.code.function(\"parse\") correctly returned Empty for markdown file")
    }
    
    @Test
    fun `code query should only work on source code files`() = runBlocking {
        // Test with Kotlin source code file
        val parser = CodeDocumentParser()
        val sourceFile = DocumentFile(
            name = "Parser.kt",
            path = "src/Parser.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sampleKotlinCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedSourceFile = parser.parse(sourceFile, sampleKotlinCode) as DocumentFile
        
        // Execute $.code.function("execute") on source code - should return results
        val sourceQuery = parseDocQL("$.code.function(\"execute\")")
        val sourceExecutor = DocQLExecutor(parsedSourceFile, parser)
        val sourceResult = sourceExecutor.execute(sourceQuery)
        
        assertTrue(
            sourceResult is DocQLResult.Chunks,
            "$.code.* queries should return results for source code files, got: $sourceResult"
        )
        println("✅ $.code.function(\"execute\") correctly returned results for source code file")
    }
    
    @Test
    fun `code classes query should return empty for non-source-code files`() = runBlocking {
        val markdownContent = """
            # Class Design
            
            ## UserService Class
            
            The UserService class handles user operations.
        """.trimIndent()
        
        val parser = MarkdownDocumentParser()
        val file = DocumentFile(
            name = "design.md",
            path = "docs/design.md",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = markdownContent.length.toLong(),
                formatType = DocumentFormatType.MARKDOWN
            )
        )
        
        val parsedFile = parser.parse(file, markdownContent) as DocumentFile
        
        // Execute $.code.classes[*] - should return Empty for markdown files
        val query = parseDocQL("$.code.classes[*]")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        assertTrue(
            result is DocQLResult.Empty,
            "$.code.classes[*] should return Empty for markdown files, but got: $result"
        )
        println("✅ $.code.classes[*] correctly returned Empty for markdown file")
    }
}

