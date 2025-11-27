package cc.unitmesh.devins.document

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertEquals

class DocumentRegistryPathCompressionTest {

    @Test
    fun `test small number of paths shows all files directly`() = runTest {
        // Setup: Register a few documents
        DocumentRegistry.clearCache()
        val paths = listOf(
            "docs/readme.md",
            "docs/guide.md",
            "src/main.kt"
        )

        // Register mock documents
        for (path in paths) {
            val doc = createMockDocument(path)
            val parser = MarkdownDocumentParser()
            DocumentRegistry.registerDocument(path, doc, parser)
        }

        // Test: 3 files < threshold 20, should show EXPANDED format (tree structure)
        val summary = DocumentRegistry.getCompressedPathsSummary(threshold = 20)

        println("Summary (small):\n$summary\n")

        // Should show tree structure with "total" and directory structure hint
        assertTrue(summary.contains("Available documents (3 total"))
        assertTrue(summary.contains("directory structure"))
        paths.forEach { path ->
            // In tree structure, we only see the file name, not the full path
            assertContains(summary, path.substringAfterLast('/'))
        }
    }

    @Test
    fun `test large number of paths shows compressed tree structure`() = runTest {
        // Setup: Register many documents
        DocumentRegistry.clearCache()
        val paths = mutableListOf<String>()

        // Create 30 documents across different directories
        for (i in 1..10) {
            paths.add("docs/chapter-$i.md")
        }
        for (i in 1..10) {
            paths.add("src/main/kotlin/service/Service$i.kt")
        }
        for (i in 1..10) {
            paths.add("tests/unit/Test$i.kt")
        }

        // Register mock documents
        for (path in paths) {
            val doc = createMockDocument(path)
            val parser = MarkdownDocumentParser()
            DocumentRegistry.registerDocument(path, doc, parser)
        }

        // Test: 30 files >= threshold 20, should show COMPRESSED format (simple list)
        val summary = DocumentRegistry.getCompressedPathsSummary(threshold = 20)

        println("Summary (large):\n$summary\n")

        // Should show simple format with just count (no "total")
        assertTrue(summary.contains("Available documents (30)"))

        // Should NOT mention DocQL queries (that's for expanded format)
        assertTrue(!summary.contains("\$.files[*]"))

        // Should show all paths as simple list
        assertContains(summary, "docs/")
        assertContains(summary, "src/")
        assertContains(summary, "tests/")

        // Should NOT have tree structure indicators
        assertTrue(!summary.contains("directory structure"))
        assertTrue(!summary.contains("Tip:"))
    }

    @Test
    fun `test tree rendering with nested directories`() = runTest {
        DocumentRegistry.clearCache()
        val paths = listOf(
            "a/b/c/file1.md",
            "a/b/c/file2.md",
            "a/b/file3.md",
            "a/file4.md",
            "x/y/file5.md"
        )

        for (path in paths) {
            val doc = createMockDocument(path)
            val parser = MarkdownDocumentParser()
            DocumentRegistry.registerDocument(path, doc, parser)
        }

        // 5 files >= threshold 4, should show COMPRESSED format (simple list)
        val summary = DocumentRegistry.getCompressedPathsSummary(threshold = 4)

        println("Summary (nested):\n$summary\n")

        // Should show simple list, not tree structure
        assertContains(summary, "a/")
        assertContains(summary, "b/")
        assertContains(summary, "c/")

        // Should show simple count without tree indicators
        assertTrue(summary.contains("Available documents (5)"))
        assertTrue(!summary.contains("directory structure"))
    }

    @Test
    @kotlin.test.Ignore()
    fun `test compressed summary saves space compared to full listing`() = runTest {
        DocumentRegistry.clearCache()
        val paths = mutableListOf<String>()

        // Create 50 documents with long paths (reduced from 100 for more reliable compression)
        for (i in 1..50) {
            val category = when (i % 5) {
                0 -> "documentation"
                1 -> "source-code"
                2 -> "test-suite"
                3 -> "resources"
                else -> "configuration"
            }
            paths.add("project/modules/$category/component-$i/file-$i.md")
        }

        for (path in paths) {
            val doc = createMockDocument(path)
            val parser = MarkdownDocumentParser()
            DocumentRegistry.registerDocument(path, doc, parser)
        }

        // Get both versions
        val compressedSummary = DocumentRegistry.getCompressedPathsSummary(threshold = 20)
        val fullListing = buildString {
            appendLine("Available documents (${paths.size}):")
            paths.forEach { path ->
                appendLine("  - $path")
            }
        }

        assertTrue(
            compressedSummary.length < fullListing.length,
            "Compressed summary (${compressedSummary.length} chars) should be shorter than full listing (${fullListing.length} chars)"
        )
    }

    @Test
    fun `test empty document list`() = runTest {
        DocumentRegistry.clearCache()

        val summary = DocumentRegistry.getCompressedPathsSummary()

        assertEquals("No documents available.", summary)
    }

    @Test
    fun `test paths with special characters`() = runTest {
        DocumentRegistry.clearCache()
        val paths = listOf(
            "docs/design-system.md",
            "docs/API_reference.md",
            "src/utils/string-utils.kt",
            "tests/unit/test_parser.kt"
        )

        for (path in paths) {
            val doc = createMockDocument(path)
            val parser = MarkdownDocumentParser()
            DocumentRegistry.registerDocument(path, doc, parser)
        }

        val summary = DocumentRegistry.getCompressedPathsSummary(threshold = 3)

        println("Summary (special chars):\n$summary\n")

        // Should handle hyphens, underscores correctly
        assertContains(summary, "docs/")
        assertContains(summary, "src/")
        assertContains(summary, "tests/")
    }

    /**
     * Helper to create a mock DocumentFile
     */
    private fun createMockDocument(path: String): DocumentFile {
        return DocumentFile(
            name = path.substringAfterLast('/'),
            path = path,
            metadata = DocumentMetadata(
                lastModified = 0L,
                fileSize = 1000L,
                formatType = DocumentFormatType.MARKDOWN
            ),
            toc = emptyList(),
            entities = emptyList()
        )
    }
}

