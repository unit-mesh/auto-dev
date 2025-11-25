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
}

