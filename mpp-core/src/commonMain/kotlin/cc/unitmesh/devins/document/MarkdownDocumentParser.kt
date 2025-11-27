package cc.unitmesh.devins.document

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

/**
 * Real Markdown parser using JetBrains Markdown library
 * Generates hierarchical TOC and supports HeadingQL/ChapterQL queries
 */
class MarkdownDocumentParser : DocumentParserService {
    private var currentContent: String? = null
    private var currentChunks: List<DocumentChunk> = emptyList()
    private var currentToc: List<TOCItem> = emptyList()
    private var chapterIdToChunk: Map<String, DocumentChunk> = emptyMap()

    override fun getDocumentContent(): String? = currentContent

    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        currentContent = content
        
        // Parse Markdown using JetBrains Markdown library
        val flavour = CommonMarkFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)
        
        val headings = extractHeadings(parsedTree, content)

        currentToc = buildHierarchicalTOC(headings, content)
        currentChunks = buildDocumentChunks(headings, content, file.path)

        chapterIdToChunk = buildChapterIdMapping(headings, currentChunks)

        return file.copy(
            toc = currentToc,
            metadata = file.metadata.copy(
                parseStatus = ParseStatus.PARSED,
                chapterCount = currentToc.size
            )
        )
    }

    override suspend fun queryHeading(keyword: String): List<DocumentChunk> {
        return currentChunks.filter { 
            it.chapterTitle?.contains(keyword, ignoreCase = true) == true ||
            it.content.contains(keyword, ignoreCase = true)
        }.sortedByDescending { 
            // Relevance scoring: title match > content match
            when {
                it.chapterTitle?.equals(keyword, ignoreCase = true) == true -> 10
                it.chapterTitle?.contains(keyword, ignoreCase = true) == true -> 5
                else -> 1
            }
        }
    }

    override suspend fun queryChapter(chapterId: String): DocumentChunk? {
        return chapterIdToChunk[chapterId]
            ?: currentChunks.find { it.anchor == chapterId || it.anchor == "#$chapterId" }
    }

    /**
     * Extract all headings from Markdown AST
     */
    private fun extractHeadings(node: ASTNode, content: String): List<HeadingInfo> {
        val headings = mutableListOf<HeadingInfo>()
        
        fun traverse(node: ASTNode) {
            if (node.type == MarkdownElementTypes.ATX_1 ||
                node.type == MarkdownElementTypes.ATX_2 ||
                node.type == MarkdownElementTypes.ATX_3 ||
                node.type == MarkdownElementTypes.ATX_4 ||
                node.type == MarkdownElementTypes.ATX_5 ||
                node.type == MarkdownElementTypes.ATX_6 ||
                node.type == MarkdownElementTypes.SETEXT_1 ||
                node.type == MarkdownElementTypes.SETEXT_2) {
                
                // Determine heading level
                val level = when (node.type) {
                    MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> 1
                    MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> 2
                    MarkdownElementTypes.ATX_3 -> 3
                    MarkdownElementTypes.ATX_4 -> 4
                    MarkdownElementTypes.ATX_5 -> 5
                    MarkdownElementTypes.ATX_6 -> 6
                    else -> 1
                }
                
                // Extract heading text - get the text content directly from the node
                val fullText = node.getTextInNode(content).toString()
                // Remove leading # characters and trim
                val text = fullText.replace(Regex("^#+\\s*"), "").replace(Regex("\\s*#+$"), "").trim()
                
                if (text.isNotEmpty()) {
                    headings.add(HeadingInfo(
                        level = level,
                        text = text,
                        startOffset = node.startOffset,
                        endOffset = node.endOffset
                    ))
                }
            }
            
            node.children.forEach { traverse(it) }
        }
        
        traverse(node)
        return headings
    }

    /**
     * Build hierarchical TOC from flat heading list
     * Now includes line numbers calculated from heading offsets
     */
    private fun buildHierarchicalTOC(headings: List<HeadingInfo>, content: String): List<TOCItem> {
        if (headings.isEmpty()) return emptyList()
        
        val root = mutableListOf<TOCItem>()
        val stack = mutableListOf<Pair<Int, MutableList<TOCItem>>>() // level to children list
        
        headings.forEachIndexed { index, heading ->
            val anchor = "#${heading.text.lowercase().replace(Regex("[^a-z0-9]+"), "-")}"
            
            // Calculate line number from offset (1-indexed)
            val lineNumber = content.take(minOf(heading.startOffset, content.length)).count { it == '\n' } + 1

            // Extract content for this section (up to next heading or end of file)
            val safeEndOffset = minOf(heading.endOffset, content.length)
            val startLine = content.take(safeEndOffset).count { it == '\n' }
            val endLine = if (index < headings.size - 1) {
                val safeNextStartOffset = minOf(headings[index + 1].startOffset, content.length)
                content.take(safeNextStartOffset).count { it == '\n' } - 1
            } else {
                content.lines().size - 1
            }

            val sectionContent = if (startLine < content.lines().size) {
                 content.lines()
                    .subList(startLine + 1, minOf(endLine + 1, content.lines().size))
                    .joinToString("\n")
                    .trim()
            } else {
                ""
            }
            
            val tocItem = TOCItem(
                level = heading.level,
                title = heading.text,
                anchor = anchor,
                lineNumber = lineNumber,
                content = sectionContent,
                children = mutableListOf()
            )
            
            // Find the right parent based on level
            while (stack.isNotEmpty() && stack.last().first >= heading.level) {
                stack.removeLast()
            }
            
            if (stack.isEmpty()) {
                // Top-level heading
                root.add(tocItem)
                stack.add(heading.level to (tocItem.children as MutableList<TOCItem>))
            } else {
                // Add as child to the last lower-level heading
                stack.last().second.add(tocItem)
                stack.add(heading.level to (tocItem.children as MutableList<TOCItem>))
            }
        }
        
        return root
    }

    /**
     * Build document chunks with content between headings
     * Includes position metadata for source attribution
     */
    private fun buildDocumentChunks(
        headings: List<HeadingInfo>, 
        content: String,
        documentPath: String
    ): List<DocumentChunk> {
        if (headings.isEmpty()) return emptyList()
        
        val chunks = mutableListOf<DocumentChunk>()
        val lines = content.lines()
        
        headings.forEachIndexed { index, heading ->
            // Calculate line numbers with bounds checking
            val safeEndOffset = minOf(heading.endOffset, content.length)
            val startLine = content.substring(0, safeEndOffset).count { it == '\n' }
            val endLine = if (index < headings.size - 1) {
                val safeNextStartOffset = minOf(headings[index + 1].startOffset, content.length)
                content.substring(0, safeNextStartOffset).count { it == '\n' } - 1
            } else {
                lines.size - 1
            }
            
            // Extract content between this heading and the next
            val chunkContent = lines
                .subList(startLine + 1, minOf(endLine + 1, lines.size))
                .joinToString("\n")
                .trim()
            
            val anchor = "#${heading.text.lowercase().replace(Regex("[^a-z0-9]+"), "-")}"
            
            // Create position metadata with bounds checking
            val positionMetadata = PositionMetadata(
                documentPath = documentPath,
                formatType = DocumentFormatType.MARKDOWN,
                position = DocumentPosition.LineRange(
                    startLine = startLine,
                    endLine = endLine,
                    startOffset = minOf(heading.startOffset, content.length),
                    endOffset = if (index < headings.size - 1) {
                        minOf(headings[index + 1].startOffset - 1, content.length)
                    } else {
                        content.length
                    }
                )
            )
            
            chunks.add(DocumentChunk(
                documentPath = documentPath,
                chapterTitle = heading.text,
                content = chunkContent,
                anchor = anchor,
                startLine = startLine,
                endLine = endLine,
                position = positionMetadata
            ))
        }
        
        return chunks
    }

    /**
     * Build chapter ID to chunk mapping for ChapterQL queries
     * Supports hierarchical chapter IDs like "1", "1.2", "1.2.3"
     */
    private fun buildChapterIdMapping(
        headings: List<HeadingInfo>,
        chunks: List<DocumentChunk>
    ): Map<String, DocumentChunk> {
        val mapping = mutableMapOf<String, DocumentChunk>()
        val chapterNumbers = mutableListOf<Int>() // Current chapter number at each level
        
        headings.forEachIndexed { index, heading ->
            val level = heading.level
            
            // Adjust chapter numbers for current level
            if (level > chapterNumbers.size) {
                while (level > chapterNumbers.size) {
                    chapterNumbers.add(1)
                }
            } else {
                while (level < chapterNumbers.size) {
                    chapterNumbers.removeLast()
                }
                if (chapterNumbers.isNotEmpty()) {
                    chapterNumbers[chapterNumbers.lastIndex]++
                } else {
                    chapterNumbers.add(1)
                }
            }
            
            val chapterId = chapterNumbers.joinToString(".")
            mapping[chapterId] = chunks[index]
        }
        
        return mapping
    }

    /**
     * Internal data class for heading information
     */
    private data class HeadingInfo(
        val level: Int,
        val text: String,
        val startOffset: Int,
        val endOffset: Int
    )
}
