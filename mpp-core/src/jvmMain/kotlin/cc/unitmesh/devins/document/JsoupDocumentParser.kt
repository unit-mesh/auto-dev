package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

private val logger = KotlinLogging.logger {}

/**
 * Jsoup-based HTML document parser for JVM platform
 * 
 * Parses HTML documents and extracts:
 * - Plain text content from the body
 * - Table of Contents from heading elements (h1-h6)
 * - Metadata from meta tags and title
 * - Document chunks based on sections
 * 
 * This parser provides structured access to HTML content with position tracking.
 */
class JsoupDocumentParser : DocumentParserService {
    private var currentContent: String? = null
    private var currentChunks: List<DocumentChunk> = emptyList()
    private var currentDocument: org.jsoup.nodes.Document? = null
    
    override fun getDocumentContent(): String? = currentContent
    
    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        logger.info { "=== Starting Jsoup HTML Parse ===" }
        logger.info { "File: ${file.path}, Size: ${content.length} bytes" }
        
        try {
            // Parse HTML document
            val doc = Jsoup.parse(content)
            currentDocument = doc
            
            // Extract text content from body (excluding head)
            val extractedText = doc.body().text()
            currentContent = extractedText
            
            logger.info { "Extracted ${extractedText.length} characters from HTML body" }
            
            // Extract metadata
            val title = doc.title()
            logger.debug { "Document title: $title" }
            
            // Build TOC from heading elements
            val toc = extractTOCFromHeadings(doc)
            logger.info { "Extracted ${toc.size} TOC items from headings" }
            
            // Build document chunks based on sections
            currentChunks = buildChunksFromSections(doc, file.path)
            logger.info { "Created ${currentChunks.size} document chunks" }
            
            logger.info { "=== HTML Parse Complete ===" }
            
            return file.copy(
                toc = toc,
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSED,
                    chapterCount = toc.size,
                    mimeType = "text/html"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse HTML document: ${e.message}" }
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
            // Relevance scoring: exact title match > title contains > content contains
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
     * Extract Table of Contents from HTML heading elements (h1-h6)
     */
    private fun extractTOCFromHeadings(doc: org.jsoup.nodes.Document): List<TOCItem> {
        val toc = mutableListOf<TOCItem>()
        
        // Select all heading elements
        val headings = doc.select("h1, h2, h3, h4, h5, h6")
        
        headings.forEachIndexed { index, heading ->
            val level = heading.tagName().substring(1).toIntOrNull() ?: 1
            val title = heading.text()
            
            // Generate anchor from id attribute or from title
            val anchor = if (heading.hasAttr("id")) {
                "#${heading.attr("id")}"
            } else {
                "#${title.lowercase().replace(Regex("[^a-z0-9]+"), "-")}"
            }
            
            toc.add(TOCItem(
                level = level,
                title = title,
                anchor = anchor,
                lineNumber = index
            ))
        }
        
        return toc
    }
    
    /**
     * Build document chunks based on sections defined by headings
     * Each chunk represents content between consecutive headings
     */
    private fun buildChunksFromSections(
        doc: org.jsoup.nodes.Document,
        documentPath: String
    ): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        
        // Get all heading elements
        val headings = doc.select("h1, h2, h3, h4, h5, h6")
        
        if (headings.isEmpty()) {
            // No headings found, create a single chunk with all body content
            val bodyText = doc.body().text()
            if (bodyText.isNotBlank()) {
                chunks.add(DocumentChunk(
                    documentPath = documentPath,
                    chapterTitle = null,
                    content = bodyText,
                    anchor = "#content",
                    position = PositionMetadata(
                        documentPath = documentPath,
                        formatType = DocumentFormatType.HTML,
                        position = DocumentPosition.LineRange(
                            startLine = 0,
                            endLine = 0
                        )
                    )
                ))
            }
            return chunks
        }
        
        // Process each heading and extract content until next heading
        headings.forEachIndexed { index, heading ->
            val title = heading.text()
            val anchor = if (heading.hasAttr("id")) {
                "#${heading.attr("id")}"
            } else {
                "#${title.lowercase().replace(Regex("[^a-z0-9]+"), "-")}"
            }
            
            // Collect content between this heading and the next one
            val content = StringBuilder()
            var currentElement: Element? = heading.nextElementSibling()
            
            while (currentElement != null) {
                // Stop if we encounter another heading
                if (currentElement.tagName().matches(Regex("h[1-6]"))) {
                    break
                }
                
                // Add element text to content
                val elementText = currentElement.text()
                if (elementText.isNotBlank()) {
                    if (content.isNotEmpty()) {
                        content.append("\n")
                    }
                    content.append(elementText)
                }
                
                currentElement = currentElement.nextElementSibling()
            }
            
            // Always create chunk for heading, even if there's no content
            // This ensures headings without direct content can still be found
            val chunkContent = content.toString().trim()
            chunks.add(DocumentChunk(
                documentPath = documentPath,
                chapterTitle = title,
                content = chunkContent,
                anchor = anchor,
                startLine = index,
                endLine = index,
                position = PositionMetadata(
                    documentPath = documentPath,
                    formatType = DocumentFormatType.HTML,
                    position = DocumentPosition.LineRange(
                        startLine = index,
                        endLine = index
                    )
                )
            ))
        }
        
        return chunks
    }
}
