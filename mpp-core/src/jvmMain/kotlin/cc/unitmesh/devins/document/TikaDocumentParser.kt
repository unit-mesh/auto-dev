package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

/**
 * Apache Tika-based document parser for JVM platform
 * 
 * Supports multiple document formats:
 * - PDF (.pdf)
 * - Microsoft Word (.doc, .docx)
 * - Microsoft PowerPoint (.ppt, .pptx)
 * - Plain text (.txt)
 * - HTML (.html)
 * - And many more formats supported by Tika
 * 
 * This parser extracts plain text content and basic metadata from documents,
 * with position information tracked where possible.
 */
class TikaDocumentParser : DocumentParserService {
    private var currentContent: String? = null
    private var currentChunks: List<DocumentChunk> = emptyList()
    private var currentMetadata: Metadata? = null
    
    override fun getDocumentContent(): String? = currentContent
    
    /**
     * Parse document from String content (for backward compatibility)
     * This method converts string back to bytes - use parseBytes() for binary files
     */
    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        logger.info { "=== Starting Tika Parse (from String) ===" }
        logger.info { "File: ${file.path}, Size: ${content.length} bytes" }
        
        // Convert String back to bytes using ISO_8859_1
        // Note: This may corrupt binary data if the string was read as UTF-8
        val bytes = content.toByteArray(Charsets.ISO_8859_1)
        return parseBytes(file, bytes)
    }
    
    /**
     * Parse document from ByteArray (preferred for binary files)
     * This method properly handles binary data without corruption
     */
    suspend fun parseBytes(file: DocumentFile, bytes: ByteArray): DocumentTreeNode {
        logger.info { "=== Starting Tika Parse (from Bytes) ===" }
        logger.info { "File: ${file.path}, Size: ${bytes.size} bytes" }
        
        try {
            // Create Tika parser components
            val parser = AutoDetectParser()
            val handler = BodyContentHandler(-1) // No limit on content size
            val metadata = Metadata()
            val context = ParseContext()
            
            // Set file name in metadata for better format detection
            metadata.set("resourceName", file.name)
            
            // Parse document directly from bytes
            val inputStream = ByteArrayInputStream(bytes)
            parser.parse(inputStream, handler, metadata, context)
            
            // Extract parsed content
            val extractedText = handler.toString().trim()
            currentContent = extractedText
            currentMetadata = metadata
            
            logger.info { "Extracted ${extractedText.length} characters" }
            logger.debug { "Metadata: ${metadata.names().joinToString { "$it=${metadata.get(it)}" }}" }
            
            // Build simple chunks (split by paragraphs or sections)
            currentChunks = buildSimpleChunks(extractedText, file.path, file.metadata.formatType)
            logger.info { "Created ${currentChunks.size} document chunks" }
            
            // Extract basic TOC if possible (for now, just return empty)
            // TODO: Enhance with more sophisticated TOC extraction based on document structure
            val toc = extractSimpleTOC(extractedText)
            
            logger.info { "=== Parse Complete ===" }
            
            return file.copy(
                toc = toc,
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSED,
                    chapterCount = toc.size,
                    mimeType = metadata.get(Metadata.CONTENT_TYPE)
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse document: ${e.message}" }
            return file.copy(
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSE_FAILED
                )
            )
        }
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
        return currentChunks.find { 
            it.anchor == chapterId || it.anchor == "#$chapterId" 
        }
    }
    
    /**
     * Build simple document chunks by splitting on double newlines (paragraphs)
     * Each chunk includes position metadata for source attribution
     */
    private fun buildSimpleChunks(
        content: String, 
        documentPath: String,
        formatType: DocumentFormatType
    ): List<DocumentChunk> {
        if (content.isBlank()) return emptyList()
        
        val chunks = mutableListOf<DocumentChunk>()
        val lines = content.lines()
        
        // Split into paragraphs (double newline or multiple empty lines)
        val paragraphs = mutableListOf<String>()
        var currentParagraph = StringBuilder()
        var emptyLineCount = 0
        
        for (line in lines) {
            if (line.isBlank()) {
                emptyLineCount++
                if (emptyLineCount >= 2 && currentParagraph.isNotEmpty()) {
                    paragraphs.add(currentParagraph.toString().trim())
                    currentParagraph = StringBuilder()
                }
            } else {
                emptyLineCount = 0
                if (currentParagraph.isNotEmpty()) {
                    currentParagraph.append("\n")
                }
                currentParagraph.append(line)
            }
        }
        
        // Add last paragraph
        if (currentParagraph.isNotEmpty()) {
            paragraphs.add(currentParagraph.toString().trim())
        }
        
        // Create chunks with position metadata
        var currentLineOffset = 0
        paragraphs.forEachIndexed { index, paragraph ->
            if (paragraph.isNotBlank()) {
                val lineCount = paragraph.count { it == '\n' } + 1
                val startLine = currentLineOffset
                val endLine = currentLineOffset + lineCount - 1
                
                // Try to extract a title from first line if it looks like a heading
                val firstLine = paragraph.lines().first()
                val title = if (firstLine.length < 100 && 
                    (firstLine.endsWith(":") || firstLine.all { it.isUpperCase() || it.isWhitespace() })) {
                    firstLine.trim()
                } else {
                    null
                }
                
                val positionMetadata = PositionMetadata(
                    documentPath = documentPath,
                    formatType = formatType,
                    position = DocumentPosition.LineRange(
                        startLine = startLine,
                        endLine = endLine
                    )
                )
                
                chunks.add(DocumentChunk(
                    documentPath = documentPath,
                    chapterTitle = title,
                    content = paragraph,
                    anchor = "#chunk-$index",
                    startLine = startLine,
                    endLine = endLine,
                    position = positionMetadata
                ))
                
                currentLineOffset = endLine + 1
            }
        }
        
        return chunks
    }
    
    /**
     * Extract simple TOC from document content
     * Looks for lines that appear to be headings (all caps, short lines, etc.)
     * This is a basic implementation; more sophisticated parsing could be added
     */
    private fun extractSimpleTOC(content: String): List<TOCItem> {
        val toc = mutableListOf<TOCItem>()
        val lines = content.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            // Simple heuristic: line is short, ends with colon, or is all uppercase
            if (trimmed.isNotEmpty() && 
                trimmed.length < 100 &&
                (trimmed.endsWith(":") || 
                 (trimmed.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".,()[]" }))) {
                toc.add(TOCItem(
                    level = 1,
                    title = trimmed,
                    anchor = "#${trimmed.lowercase().replace(Regex("[^a-z0-9]+"), "-")}",
                    lineNumber = index
                ))
            }
        }
        
        return toc
    }
}


