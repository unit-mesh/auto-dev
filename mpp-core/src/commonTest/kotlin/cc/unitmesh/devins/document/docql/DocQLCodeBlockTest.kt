package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for DocQL codeblock and structure queries
 */
class DocQLCodeBlockTest {
    
    /**
     * Create a mock parser service that returns markdown content with code blocks
     */
    private class MockMarkdownParser(private val content: String) : DocumentParserService {
        override fun getDocumentContent(): String = content
        
        override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
            return file
        }
        
        override suspend fun queryHeading(keyword: String): List<DocumentChunk> = emptyList()
        
        override suspend fun queryChapter(chapterId: String): DocumentChunk? = null
    }
    
    private fun createTestDocument(): DocumentFile {
        return DocumentFile(
            name = "test.md",
            path = "/test/test.md",
            metadata = DocumentMetadata(
                lastModified = 0,
                fileSize = 1000,
                language = "markdown"
            )
        )
    }
    
    private val markdownWithCodeBlocks = """
# Test Document

Some introductory text.

## Kotlin Example

```kotlin
fun hello() {
    println("Hello, World!")
}
```

## Java Example

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello!");
    }
}
```

## Python Example

```python
def hello():
    print("Hello!")
```

## Unlabeled Code

```
plain text code
without language
```
    """.trimIndent()
    
    @Test
    fun `test extract all code blocks`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(4, result.items.size)
    }
    
    @Test
    fun `test extract first code block`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertEquals("kotlin", result.items[0].language)
        assertTrue(result.items[0].code.contains("println"))
    }
    
    @Test
    fun `test filter code blocks by language equals`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.language=="kotlin")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertEquals("kotlin", result.items[0].language)
    }
    
    @Test
    fun `test filter code blocks by language contains`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.language~="java")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertEquals("java", result.items[0].language)
    }
    
    @Test
    fun `test filter code blocks by code contains`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.code~="println")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        // Should match kotlin and java (both have println)
        assertEquals(2, result.items.size)
    }
    
    @Test
    fun `test code block with no language`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[3]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertEquals(null, result.items[0].language)
        assertTrue(result.items[0].code.contains("plain text"))
    }
    
    @Test
    fun `test code block location has line number`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[0]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertTrue(result.items[0].location.line != null)
        assertTrue(result.items[0].location.line!! > 0)
    }
    
    @Test
    fun `test empty content returns empty result`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser("")
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Empty>(result)
    }
    
    @Test
    fun `test content without code blocks returns empty`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser("# Just a heading\n\nSome text without code blocks.")
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.content.codeblock[*]")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Empty>(result)
    }
    
    @Test
    fun `test filter by non-matching language returns empty`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.language=="rust")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Empty>(result)
    }
    
    @Test
    fun `test structure query on single document`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("$.structure")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Structure>(result)
        assertEquals(1, result.fileCount)
        assertTrue(result.paths.contains("/test/test.md"))
    }
    
    @Test
    fun `test structure query without document returns error`() = runTest {
        val executor = DocQLExecutor(null, null)
        
        val query = parseDocQL("$.structure")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.Error>(result)
    }
    
    @Test
    fun `test code block filter with not equals`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.language!="kotlin")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        // Should match java, python, and the unlabeled one
        assertEquals(3, result.items.size)
        assertTrue(result.items.none { it.language == "kotlin" })
    }
    
    @Test
    fun `test code block filter starts with`() = runTest {
        val doc = createTestDocument()
        val parser = MockMarkdownParser(markdownWithCodeBlocks)
        val executor = DocQLExecutor(doc, parser)
        
        val query = parseDocQL("""$.content.codeblock[?(@.language startsWith "py")]""")
        val result = executor.execute(query)
        
        assertIs<DocQLResult.CodeBlocks>(result)
        assertEquals(1, result.items.size)
        assertEquals("python", result.items[0].language)
    }
}

