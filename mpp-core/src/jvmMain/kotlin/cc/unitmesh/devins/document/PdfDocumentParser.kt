package cc.unitmesh.devins.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Apache PDFBox-based document parser for JVM platform
 */
class PdfDocumentParser : DocumentParserService {
    private var currentContent: String? = null
    private var currentChunks: List<DocumentChunk> = emptyList()

    override fun getDocumentContent(): String? = currentContent

    override suspend fun parse(file: DocumentFile, content: String): DocumentTreeNode {
        logger.info { "=== Starting PDFBox Parse ===" }
        logger.info { "File: ${file.path}" }

        val result = try {
            val pdfFile = File(file.path)
            if (!pdfFile.exists()) {
                throw IllegalArgumentException("File not found: ${file.path}")
            }

            Loader.loadPDF(pdfFile).use { document ->
                // Extract full text
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                currentContent = fullText.trim()

                logger.info { "Extracted ${fullText.length} characters" }

                // Build chunks by page
                currentChunks = buildPageChunks(document, file.path)
                logger.info { "Created ${currentChunks.size} document chunks" }

                // Extract TOC
                val toc = extractTOC(document)
                logger.info { "Extracted ${toc.size} TOC items" }

                logger.info { "=== Parse Complete ===" }

                file.copy(
                    toc = toc,
                    metadata = file.metadata.copy(
                        parseStatus = ParseStatus.PARSED,
                        chapterCount = toc.size,
                        totalPages = document.numberOfPages,
                        mimeType = "application/pdf",
                        formatType = DocumentFormatType.PDF
                    )
                )
            }
        } catch (e: Exception) {
            logger.error { "Failed to parse PDF: ${e.message}" }
            file.copy(
                metadata = file.metadata.copy(
                    parseStatus = ParseStatus.PARSE_FAILED
                )
            )
        }
        
        return result
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

    private fun buildPageChunks(document: PDDocument, documentPath: String): List<DocumentChunk> {
        val chunks = mutableListOf<DocumentChunk>()
        val stripper = PDFTextStripper()

        for (pageIndex in 0 until document.numberOfPages) {
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            
            try {
                val pageText = stripper.getText(document).trim()
                if (pageText.isNotEmpty()) {
                    chunks.add(
                        DocumentChunk(
                            documentPath = documentPath,
                            chapterTitle = "Page ${pageIndex + 1}",
                            content = pageText,
                            anchor = "#page-${pageIndex + 1}",
                            page = pageIndex + 1,
                            position = PositionMetadata(
                                documentPath = documentPath,
                                formatType = DocumentFormatType.PDF,
                                position = DocumentPosition.PageRange(pageIndex + 1, pageIndex + 1)
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                logger.warn { "Failed to extract text from page ${pageIndex + 1}: ${e.message}" }
            }
        }
        return chunks
    }

    private fun extractTOC(document: PDDocument): List<TOCItem> {
        val outline = document.documentCatalog.documentOutline ?: return emptyList()
        val toc = mutableListOf<TOCItem>()
        
        var currentItem = outline.firstChild
        while (currentItem != null) {
            processOutlineItem(currentItem, 1, toc)
            currentItem = currentItem.nextSibling
        }
        
        return toc
    }

    private fun processOutlineItem(item: PDOutlineItem, level: Int, list: MutableList<TOCItem>) {
        val title = item.title ?: "Untitled"
        val children = mutableListOf<TOCItem>()
        
        var child = item.firstChild
        while (child != null) {
            processOutlineItem(child, level + 1, children)
            child = child.nextSibling
        }

        list.add(TOCItem(
            level = level,
            title = title,
            anchor = "#${title.lowercase().replace(Regex("[^a-z0-9]+"), "-")}",
            children = children
        ))
    }
}
