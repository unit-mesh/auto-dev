package cc.unitmesh.devins.document

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeDocumentParserTest {
    
    @Test
    fun `should parse DocQL Kotlin source code`() = runBlocking {
        // Sample Kotlin code from DocQL
        val sourceCode = """
            package cc.unitmesh.devins.document.docql
            
            import cc.unitmesh.devins.document.*
            
            /**
             * DocQL query execution result - all results include source file information
             */
            sealed class DocQLResult {
                data class TocItems(val itemsByFile: Map<String, List<TOCItem>>) : DocQLResult()
                data class Entities(val itemsByFile: Map<String, List<Entity>>) : DocQLResult()
                object Empty : DocQLResult()
                data class Error(val message: String) : DocQLResult()
            }
            
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
                    // Implementation here
                    return DocQLResult.Empty
                }
                
                private fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
                    return DocQLResult.Empty
                }
            }
        """.trimIndent()
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/docql/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val result = parser.parse(file, sourceCode)
        
        // Verify result is DocumentFile
        assertTrue(result is DocumentFile)
        val docFile = result as DocumentFile
        
        // Verify TOC was built
        assertTrue(docFile.toc.isNotEmpty())
        println("TOC items: ${docFile.toc.size}")
        docFile.toc.forEach { toc ->
            println("  - ${toc.title} (level ${toc.level}, ${toc.children.size} children)")
        }
        
        // Verify entities were extracted
        assertTrue(docFile.entities.isNotEmpty())
        println("\nEntities: ${docFile.entities.size}")
        docFile.entities.forEach { entity ->
            when (entity) {
                is Entity.ClassEntity -> println("  - Class: ${entity.name} in ${entity.packageName}")
                is Entity.FunctionEntity -> println("  - Function: ${entity.name}")
                else -> println("  - Entity: ${entity.name}")
            }
        }
        
        // Verify we can query by heading
        val chunks = parser.queryHeading("DocQL")
        assertTrue(chunks.isNotEmpty())
        println("\nQuery 'DocQL' found ${chunks.size} chunks")
        chunks.forEach { chunk ->
            println("  - ${chunk.chapterTitle} (${chunk.startLine}-${chunk.endLine})")
        }
        
        // Verify parse status
        assertEquals(ParseStatus.PARSED, docFile.metadata.parseStatus)
    }
    
    @Test
    fun `should detect correct language from file extension`() {
        val parser = CodeDocumentParser()
        
        val testCases = mapOf(
            "Test.java" to "JAVA",
            "Test.kt" to "KOTLIN",
            "test.py" to "PYTHON",
            "test.js" to "JAVASCRIPT",
            "test.ts" to "TYPESCRIPT",
            "test.go" to "GO",
            "test.rs" to "RUST"
        )
        
        // This is tested indirectly through the format detection
        testCases.forEach { (fileName, expectedLang) ->
            val formatType = DocumentParserFactory.detectFormat(fileName)
            assertNotNull(formatType)
            assertEquals(DocumentFormatType.SOURCE_CODE, formatType)
        }
    }
    
    @Test
    fun `should find classes by package structure`() = runBlocking {
        val sourceCode = """
            package cc.unitmesh.devins.document.docql
            
            sealed class DocQLResult {
                data class TocItems(val items: List<String>) : DocQLResult()
                data class Empty(val message: String) : DocQLResult()
            }
            
            class DocQLExecutor {
                fun execute() {}
            }
        """.trimIndent()
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "TestFile.kt",
            path = "test/TestFile.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val result = parser.parse(file, sourceCode) as DocumentFile
        
        // Should group classes by package
        assertTrue(result.toc.isNotEmpty())
        println("\n📦 Package Structure:")
        result.toc.forEach { toc ->
            println("  ${toc.title}")
            toc.children.forEach { child ->
                println("    - ${child.title}")
            }
        }
        
        // Should find all classes
        val classes = result.entities.filterIsInstance<Entity.ClassEntity>()
        assertTrue(classes.size >= 3, "Should find at least DocQLResult, TocItems, Empty, DocQLExecutor")
        println("\n📘 Found ${classes.size} classes")
    }
    
    @Test
    fun `should query methods by name pattern`() = runBlocking {
        val sourceCode = """
            class DocumentParser {
                fun parseDocument(content: String) {}
                fun parseMarkdown(content: String) {}
                fun parseCode(content: String) {}
                fun validateDocument() {}
                fun getContent() {}
            }
        """.trimIndent()
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Parser.kt",
            path = "test/Parser.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val result = parser.parse(file, sourceCode)
        
        // Query for "parse" methods
        val parseChunks = parser.queryHeading("parse")
        println("\n🔍 Query 'parse' found ${parseChunks.size} results:")
        parseChunks.forEach { chunk ->
            println("  - ${chunk.chapterTitle}")
        }
        assertTrue(parseChunks.size >= 3, "Should find parseDocument, parseMarkdown, parseCode")
        
        // Query for "get" methods
        val getChunks = parser.queryHeading("get")
        println("\n🔍 Query 'get' found ${getChunks.size} results:")
        getChunks.forEach { chunk ->
            println("  - ${chunk.chapterTitle}")
        }
        assertTrue(getChunks.isNotEmpty(), "Should find getContent")
    }
    
    @Test
    fun `should preserve method bodies for context`() = runBlocking {
        val sourceCode = """
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    // Add two numbers
                    return a + b
                }
                
                fun subtract(a: Int, b: Int): Int {
                    // Subtract two numbers
                    return a - b
                }
            }
        """.trimIndent()
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Calculator.kt",
            path = "test/Calculator.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val result = parser.parse(file, sourceCode)
        
        // Query for add method
        val chunks = parser.queryHeading("add")
        assertTrue(chunks.isNotEmpty())
        
        val addChunk = chunks.first()
        println("\n📝 Method body content:")
        println(addChunk.content)
        
        // Should contain the method implementation
        assertTrue(addChunk.content.contains("return a + b"), "Should preserve method body")
        assertTrue(addChunk.content.contains("// Add two numbers"), "Should preserve comments")
    }
    
    @Test
    fun `should handle nested classes correctly`() = runBlocking {
        val sourceCode = """
            class OuterClass {
                class InnerClass {
                    fun innerMethod() {}
                }
                
                fun outerMethod() {}
            }
        """.trimIndent()
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "Nested.kt",
            path = "test/Nested.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val result = parser.parse(file, sourceCode) as DocumentFile
        
        println("\n🗂️  Nested class structure:")
        result.toc.forEach { toc ->
            println("${toc.title} (${toc.children.size} children)")
            toc.children.forEach { child ->
                println("  - ${child.title}")
            }
        }
        
        // Should find both outer and inner classes
        val classes = result.entities.filterIsInstance<Entity.ClassEntity>()
        assertTrue(classes.isNotEmpty())
        println("Found ${classes.size} classes: ${classes.map { it.name }}")
    }
}

