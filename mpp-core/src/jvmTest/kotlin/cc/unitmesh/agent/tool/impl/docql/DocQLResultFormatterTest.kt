package cc.unitmesh.agent.tool.impl.docql

import cc.unitmesh.agent.scoring.TextSegment
import cc.unitmesh.devins.document.*
import cc.unitmesh.devins.document.docql.DocQLExecutor
import cc.unitmesh.devins.document.docql.parseDocQL
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertContains

/**
 * Test suite for DocQL result formatting improvements.
 * 
 * Tests that search results display meaningful code content (up to 20 lines per function)
 * instead of just entity names.
 */
class DocQLResultFormatterTest {
    
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
                if (query.nodes.isEmpty()) {
                    return DocQLResult.Error("Empty query")
                }
                
                // First node must be Root
                if (query.nodes[0] !is DocQLNode.Root) {
                    return DocQLResult.Error("Query must start with $")
                }
                
                return DocQLResult.Empty
            }
            
            /**
             * Execute TOC query ($.toc[...])
             */
            private fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult {
                if (documentFile == null) {
                    return DocQLResult.Error("No document loaded")
                }
                
                var items = documentFile.toc
                return DocQLResult.Empty
            }
            
            /**
             * Execute code query
             */
            private fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult {
                return DocQLResult.Empty
            }
        }
        
        sealed class DocQLResult {
            data class TocItems(val items: List<String>) : DocQLResult()
            object Empty : DocQLResult()
            data class Error(val message: String) : DocQLResult()
        }
        
        class DocQLParser(private val tokens: List<DocQLToken>) {
            fun parse(): DocQLQuery {
                return DocQLQuery(emptyList())
            }
            
            private fun parseFilter(): FilterCondition {
                return FilterCondition.Equals("", "")
            }
        }
    """.trimIndent()
    
    @Test
    fun `formatFallbackResult should include code content for functions`() {
        // Create a scored result with function entity and code preview
        val functionEntity = Entity.FunctionEntity(
            name = "execute",
            signature = "suspend fun execute(query: DocQLQuery): DocQLResult",
            location = Location(anchor = "#execute", line = 15)
        )
        
        val codePreview = """
            suspend fun execute(query: DocQLQuery): DocQLResult {
                if (query.nodes.isEmpty()) {
                    return DocQLResult.Error("Empty query")
                }
                
                // First node must be Root
                if (query.nodes[0] !is DocQLNode.Root) {
                    return DocQLResult.Error("Query must start with $")
                }
                
                return DocQLResult.Empty
            }
        """.trimIndent()
        
        val result = ScoredResult(
            item = functionEntity,
            score = 95.5,
            uniqueId = "test:execute:15",
            preview = codePreview,
            filePath = "test/DocQLExecutor.kt"
        )
        
        val formatted = DocQLResultFormatter.formatFallbackResult(
            results = listOf(result),
            keyword = "execute",
            truncated = false
        )
        
        println("=== Formatted Result ===")
        println(formatted)
        println("========================")
        
        // Verify the output includes the code content
        assertTrue(formatted.contains("suspend fun execute"), "Should include function signature")
        assertTrue(formatted.contains("DocQLResult.Error"), "Should include function body")
        assertTrue(formatted.contains("```kotlin"), "Should use code blocks")
        assertTrue(formatted.contains("Function:"), "Should label as Function")
    }
    
    @Test
    fun `formatFallbackResult should show class summary with function list`() {
        val classEntity = Entity.ClassEntity(
            name = "DocQLExecutor",
            packageName = "cc.unitmesh.devins.document.docql",
            location = Location(anchor = "#DocQLExecutor", line = 8)
        )
        
        val classSummary = """
            class DocQLExecutor(
                private val documentFile: DocumentFile?,
                private val parserService: DocumentParserService?
            ) {
            
                // Functions:
                //   - suspend fun execute(query: DocQLQuery): DocQLResult:15
                //   - fun executeTocQuery(nodes: List<DocQLNode>): DocQLResult:30
                //   - fun executeCodeQuery(nodes: List<DocQLNode>): DocQLResult:45
            }
        """.trimIndent()
        
        val result = ScoredResult(
            item = classEntity,
            score = 99.0,
            uniqueId = "test:DocQLExecutor:8",
            preview = classSummary,
            filePath = "test/DocQLExecutor.kt"
        )
        
        val formatted = DocQLResultFormatter.formatFallbackResult(
            results = listOf(result),
            keyword = "DocQLExecutor",
            truncated = false
        )
        
        println("=== Class Summary Result ===")
        println(formatted)
        println("============================")
        
        // Verify class output includes function list
        assertTrue(formatted.contains("class DocQLExecutor"), "Should include class declaration")
        assertTrue(formatted.contains("execute"), "Should list execute function")
        assertTrue(formatted.contains("Class:"), "Should label as Class")
    }
    
    @Test
    fun `formatFallbackResult should truncate long functions to 20 lines`() {
        val functionEntity = Entity.FunctionEntity(
            name = "longFunction",
            signature = "fun longFunction(): Unit",
            location = Location(anchor = "#longFunction", line = 1)
        )
        
        // Create a function with more than 20 lines
        val longCode = (1..30).joinToString("\n") { "    println(\"Line $it\")" }
        val fullCode = "fun longFunction() {\n$longCode\n}"
        
        val result = ScoredResult(
            item = functionEntity,
            score = 80.0,
            uniqueId = "test:longFunction:1",
            preview = fullCode,
            filePath = "test/LongFunction.kt"
        )
        
        val formatted = DocQLResultFormatter.formatFallbackResult(
            results = listOf(result),
            keyword = "longFunction",
            truncated = false
        )
        
        println("=== Truncated Function Result ===")
        println(formatted)
        println("=================================")
        
        // Verify truncation indicator is present
        assertTrue(formatted.contains("more lines"), "Should indicate more lines available")
        // Verify it doesn't show all 30 lines
        assertTrue(!formatted.contains("Line 25"), "Should not show line 25")
    }
    
    @Test
    fun `TextSegment should include code content for entities`() = runBlocking {
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
        
        // Register the document
        DocumentRegistry.registerDocument(file.path, parsedFile, parser)
        
        // Execute search
        val query = parseDocQL("$.code.function(\"execute\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        println("=== Query Result ===")
        println(result)
        println("====================")
        
        // Collect items using the executor
        val items = mutableListOf<SearchItem>()
        val metadata = mutableMapOf<SearchItem, Pair<Any, String?>>()
        val searchExecutor = DocQLKeywordSearchExecutor()
        searchExecutor.collectSearchItems(result, items, metadata)
        
        // Verify that collected items have meaningful content
        assertTrue(items.isNotEmpty(), "Should have collected items")
        
        items.forEach { item ->
            println("Item text length: ${item.segment.text.length}")
            println("Item text preview: ${item.segment.text.take(100)}...")
            
            // The text should be more than just a function name
            assertTrue(
                item.segment.text.length > 20,
                "TextSegment should contain code content, not just name"
            )
        }
        
        // Cleanup
        DocumentRegistry.clearCache()
    }
    
    @Test
    fun `search results should group by file and show code blocks`() {
        // Create multiple results from different files
        val results = listOf(
            ScoredResult(
                item = Entity.FunctionEntity(
                    name = "execute",
                    signature = "suspend fun execute(query: DocQLQuery): DocQLResult",
                    location = Location(anchor = "#execute", line = 15)
                ),
                score = 99.0,
                uniqueId = "file1:execute:15",
                preview = "suspend fun execute(query: DocQLQuery): DocQLResult {\n    return DocQLResult.Empty\n}",
                filePath = "src/DocQLExecutor.kt"
            ),
            ScoredResult(
                item = Entity.FunctionEntity(
                    name = "parse",
                    signature = "fun parse(): DocQLQuery",
                    location = Location(anchor = "#parse", line = 5)
                ),
                score = 85.0,
                uniqueId = "file2:parse:5",
                preview = "fun parse(): DocQLQuery {\n    return DocQLQuery(emptyList())\n}",
                filePath = "src/DocQLParser.kt"
            )
        )
        
        val formatted = DocQLResultFormatter.formatFallbackResult(
            results = results,
            keyword = "parse execute",
            truncated = false
        )
        
        println("=== Grouped Results ===")
        println(formatted)
        println("=======================")
        
        // Verify grouping by file
        assertTrue(formatted.contains("### src/DocQLExecutor.kt"), "Should group by first file")
        assertTrue(formatted.contains("### src/DocQLParser.kt"), "Should group by second file")
        
        // Verify code blocks are present
        assertTrue(formatted.contains("```kotlin"), "Should use kotlin code blocks")
        assertTrue(formatted.contains("```"), "Should close code blocks")
    }
    
    @Test
    fun `formatSmartSummary should include function signatures`() {
        val results = listOf(
            ScoredResult(
                item = Entity.FunctionEntity(
                    name = "execute",
                    signature = "suspend fun execute(query: DocQLQuery)",
                    location = Location(anchor = "#execute", line = 15)
                ),
                score = 99.0,
                uniqueId = "test:execute:15",
                preview = "code...",
                filePath = "test/DocQLExecutor.kt"
            ),
            ScoredResult(
                item = Entity.ClassEntity(
                    name = "DocQLParser",
                    packageName = "cc.unitmesh",
                    location = Location(anchor = "#DocQLParser", line = 1)
                ),
                score = 85.0,
                uniqueId = "test:DocQLParser:1",
                preview = "class...",
                filePath = "test/DocQLParser.kt"
            )
        )
        
        val summary = DocQLResultFormatter.formatSmartSummary(
            results = results,
            totalCount = 2,
            truncated = false
        )
        
        println("=== Smart Summary ===")
        println(summary)
        println("=====================")
        
        assertTrue(summary.contains("execute"), "Should include function name")
        assertTrue(summary.contains("function"), "Should include type")
        assertTrue(summary.contains("DocQLParser"), "Should include class name")
    }
}

