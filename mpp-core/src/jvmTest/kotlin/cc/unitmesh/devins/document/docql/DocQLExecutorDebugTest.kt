package cc.unitmesh.devins.document.docql

import cc.unitmesh.devins.document.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Debug test for DocQL optimization analysis
 * 
 * This test helps analyze DocQL behavior with different document types,
 * including potential issues with changelog.md and other markdown files.
 */
class DocQLExecutorDebugTest {
    
    @Test
    fun `debug DocQLExecutor class query`() = runBlocking {
        // Read the actual DocQLExecutor.kt file
        val sourceCode = java.io.File("/Volumes/source/ai/autocrud/mpp-core/src/commonMain/kotlin/cc/unitmesh/devins/document/docql/DocQLExecutor.kt").readText()
        
        println("=== Source Code Stats ===")
        println("File size: ${sourceCode.length} chars")
        println("Lines: ${sourceCode.lines().size}")
        
        val parser = CodeDocumentParser()
        val file = DocumentFile(
            name = "DocQLExecutor.kt",
            path = "test/DocQLExecutor.kt",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = sourceCode.length.toLong(),
                formatType = DocumentFormatType.SOURCE_CODE
            )
        )
        
        val parsedFile = parser.parse(file, sourceCode) as DocumentFile
        
        println("\n=== Parsed Entities ===")
        println("Total entities: ${parsedFile.entities.size}")
        
        // Print all entities
        println("\nAll entities:")
        parsedFile.entities.forEachIndexed { index, entity ->
            val type = entity.javaClass.simpleName
            val name = when (entity) {
                is Entity.ClassEntity -> entity.name
                is Entity.FunctionEntity -> entity.name
                is Entity.ConstructorEntity -> entity.className
                else -> "?"
            }
            val line = entity.location.line
            println("  [$index] $type: '$name' at line $line")
        }
        
        // Print all class entities
        val classes = parsedFile.entities.filterIsInstance<Entity.ClassEntity>()
        println("\nClasses found (${classes.size}):")
        classes.forEach { cls ->
            println("  - ${cls.name} at line ${cls.location.line}")
        }
        
        // Check if DocQLExecutor is in the list
        val hasDocQLExecutor = classes.any { it.name == "DocQLExecutor" }
        println("\nHas DocQLExecutor class: $hasDocQLExecutor")
        
        // Query for DocQLExecutor
        println("\n=== Query \$.code.class(\"DocQLExecutor\") ===")
        val query = parseDocQL("\$.code.class(\"DocQLExecutor\")")
        val executor = DocQLExecutor(parsedFile, parser)
        val result = executor.execute(query)
        
        println("Result type: ${result::class.simpleName}")
        when (result) {
            is DocQLResult.Chunks -> {
                println("Found ${result.totalCount} chunks")
                result.items.forEach { chunk ->
                    println("  - chapterTitle: ${chunk.chapterTitle}")
                    println("    content preview: ${chunk.content.take(200)}...")
                }
            }
            is DocQLResult.Entities -> {
                println("Found ${result.totalCount} entities")
            }
            is DocQLResult.Empty -> {
                println("Empty result")
            }
            else -> {
                println("Other result: $result")
            }
        }
        
        // Query heading directly
        println("\n=== Direct queryHeading(\"DocQLExecutor\") ===")
        val headingChunks = parser.queryHeading("DocQLExecutor")
        println("Found ${headingChunks.size} chunks via queryHeading")
        headingChunks.forEach { chunk ->
            println("  - chapterTitle: '${chunk.chapterTitle}'")
            println("    startLine: ${chunk.startLine}, endLine: ${chunk.endLine}")
        }
        
        assertTrue(hasDocQLExecutor, "Should find DocQLExecutor class")
    }
    
    @Test
    fun `debug markdown file parsing and changelog issue`() = runBlocking {
        // Test with a typical changelog structure
        val changelogContent = """
            # Changelog
            
            All notable changes to this project will be documented in this file.
            
            ## [1.2.0] - 2024-11-27
            
            ### Added
            - New DocQL query syntax
            - Support for code class queries
            
            ### Fixed
            - Bug in class parsing
            - Issue with large files
            
            ## [1.1.0] - 2024-11-20
            
            ### Added
            - Initial DocQL support
            
            ### Changed
            - Improved performance
            
            ## [1.0.0] - 2024-11-01
            
            ### Added
            - Initial release
            - Basic document parsing
        """.trimIndent()
        
        println("=== Changelog Parsing Test ===")
        println("Content length: ${changelogContent.length} chars")
        println("Lines: ${changelogContent.lines().size}")
        
        val parser = MarkdownDocumentParser()
        val file = DocumentFile(
            name = "CHANGELOG.md",
            path = "CHANGELOG.md",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = changelogContent.length.toLong(),
                formatType = DocumentFormatType.MARKDOWN
            )
        )
        
        val parsedFile = parser.parse(file, changelogContent) as DocumentFile
        
        println("\n=== TOC Items ===")
        println("Total TOC items: ${parsedFile.toc.size}")
        parsedFile.toc.forEachIndexed { index, item ->
            val indent = "  ".repeat(item.level - 1)
            println("  $indent[$index] L${item.level}: '${item.title}'")
        }
        
        // Query for "DocQL"
        println("\n=== Query heading 'DocQL' ===")
        val docqlChunks = parser.queryHeading("DocQL")
        println("Found ${docqlChunks.size} chunks matching 'DocQL'")
        docqlChunks.forEach { chunk ->
            println("  - chapterTitle: '${chunk.chapterTitle}'")
            println("    content: ${chunk.content.take(100)}...")
        }
        
        // Query for version
        println("\n=== Query heading '1.2.0' ===")
        val versionChunks = parser.queryHeading("1.2.0")
        println("Found ${versionChunks.size} chunks matching '1.2.0'")
        versionChunks.forEach { chunk ->
            println("  - chapterTitle: '${chunk.chapterTitle}'")
            println("    content: ${chunk.content.take(100)}...")
        }
        
        assertTrue(parsedFile.toc.isNotEmpty(), "Should have TOC items")
    }
    
    @Test
    fun `debug actual CHANGELOG parsing from project`() = runBlocking {
        val changelogFile = java.io.File("/Volumes/source/ai/autocrud/CHANGELOG.md")
        if (!changelogFile.exists()) {
            println("Skipping: CHANGELOG.md not found")
            return@runBlocking
        }
        
        val content = changelogFile.readText()
        
        println("=== Actual CHANGELOG.md Stats ===")
        println("File size: ${content.length} chars")
        println("Lines: ${content.lines().size}")
        
        val parser = MarkdownDocumentParser()
        val file = DocumentFile(
            name = "CHANGELOG.md",
            path = "CHANGELOG.md",
            metadata = DocumentMetadata(
                lastModified = System.currentTimeMillis(),
                fileSize = content.length.toLong(),
                formatType = DocumentFormatType.MARKDOWN
            )
        )
        
        val parsedFile = parser.parse(file, content) as DocumentFile
        
        println("\n=== TOC Items (first 20) ===")
        println("Total TOC items: ${parsedFile.toc.size}")
        parsedFile.toc.take(20).forEachIndexed { index, item ->
            val indent = "  ".repeat(item.level - 1)
            println("  $indent[$index] L${item.level}: '${item.title.take(50)}'")
        }
        if (parsedFile.toc.size > 20) {
            println("  ... and ${parsedFile.toc.size - 20} more")
        }
        
        // Query for a common keyword
        println("\n=== Query heading 'fix' ===")
        val fixChunks = parser.queryHeading("fix")
        println("Found ${fixChunks.size} chunks matching 'fix'")
        if (fixChunks.size > 10) {
            println("  WARNING: Too many results! This could cause issues.")
        }
        fixChunks.take(5).forEach { chunk ->
            println("  - '${chunk.chapterTitle?.take(50) ?: "no title"}'")
        }
        
        assertTrue(true, "Debug test completed")
    }
    
    @Test
    fun `analyze keyword search result distribution`() = runBlocking {
        // This test analyzes how keyword search distributes results across file types
        println("=== Keyword Search Distribution Analysis ===")
        
        // Common search terms that might match changelog entries
        val problematicKeywords = listOf("fix", "add", "change", "update", "bug", "feature")
        
        println("\nProblematic keywords that match many changelog entries:")
        problematicKeywords.forEach { keyword ->
            println("  - '$keyword' - likely to match many changelog entries")
        }
        
        println("\nSuggested optimizations:")
        println("  1. Lower scoring for changelog/release notes files")
        println("  2. Skip changelog files in code-focused queries")
        println("  3. Add file type filtering in DocQL syntax")
        println("  4. Implement file exclusion patterns")
        
        assertTrue(true, "Analysis completed")
    }
    
    @Test
    fun `analyze file type scoring needs`() = runBlocking {
        println("=== File Type Scoring Analysis ===")
        
        // Files that should have lower priority in code-focused searches
        val lowPriorityPatterns = listOf(
            "CHANGELOG.md",
            "HISTORY.md",
            "RELEASE.md",
            "NEWS.md",
            "releases/",
            "node_modules/",
            ".git/",
            "dist/",
            "build/"
        )
        
        println("\nFile patterns that should have lower priority in code searches:")
        lowPriorityPatterns.forEach { pattern ->
            println("  - '$pattern'")
        }
        
        // Files that should have higher priority
        val highPriorityPatterns = listOf(
            "src/",
            "lib/",
            "core/",
            "README.md",
            "AGENTS.md",
            "docs/api/",
            "docs/guide/"
        )
        
        println("\nFile patterns that should have higher priority:")
        highPriorityPatterns.forEach { pattern ->
            println("  - '$pattern'")
        }
        
        println("\nProposed scoring adjustments:")
        println("  - CHANGELOG.md: 0.3x weight")
        println("  - Test files: 0.5x weight (unless searching for tests)")
        println("  - Source code: 1.0x weight")
        println("  - README.md: 1.2x weight (for overview queries)")
        println("  - node_modules/: skip entirely")
        
        assertTrue(true, "Analysis completed")
    }
    
    @Test
    fun `debug document registry state`() = runBlocking {
        println("=== Document Registry State ===")
        
        val registeredPaths = cc.unitmesh.devins.document.DocumentRegistry.getRegisteredPaths()
        println("Total registered documents: ${registeredPaths.size}")
        
        // Group by file type
        val byExtension = registeredPaths.groupBy { path ->
            val ext = path.substringAfterLast('.', "")
            if (ext.isEmpty()) "no_extension" else ext.lowercase()
        }
        
        println("\nBy file extension:")
        byExtension.entries.sortedByDescending { it.value.size }.take(10).forEach { (ext, paths) ->
            println("  .$ext: ${paths.size} files")
        }
        
        // Check for potentially problematic files
        val changelogFiles = registeredPaths.filter { path ->
            val name = path.substringAfterLast('/').lowercase()
            name.contains("changelog") || name.contains("history") || name.contains("release")
        }
        
        println("\nPotentially problematic changelog-like files:")
        changelogFiles.take(10).forEach { path ->
            println("  - $path")
        }
        if (changelogFiles.size > 10) {
            println("  ... and ${changelogFiles.size - 10} more")
        }
        
        assertTrue(true, "Debug completed")
    }
}

