package cc.unitmesh.devins.document

import cc.unitmesh.devins.document.pdf.PagePdfDocumentReader
import cc.unitmesh.devins.document.pdf.config.PdfDocumentReaderConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Apache PDFBox-based document parser for JVM platform.
 * This class now uses Spring AI's PagePdfDocumentReader internally for better text extraction.
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

            // Use PagePdfDocumentReader for better text extraction
            val config = PdfDocumentReaderConfig.defaultConfig()
            val reader = PagePdfDocumentReader(file.path, config)
            val pdfDocuments = reader.get()

            // Build full text from all documents
            currentContent = pdfDocuments.joinToString("\n") { it.text }
            logger.info { "Extracted ${currentContent?.length ?: 0} characters" }

            // Build chunks from PDF documents
            currentChunks = pdfDocuments.mapIndexed { index, pdfDoc ->
                val pageNumber = (pdfDoc.metadata[PagePdfDocumentReader.METADATA_START_PAGE_NUMBER] as? Int) ?: (index + 1)
                val endPageNumber = (pdfDoc.metadata[PagePdfDocumentReader.METADATA_END_PAGE_NUMBER] as? Int) ?: pageNumber
                
                DocumentChunk(
                    documentPath = file.path,
                    chapterTitle = if (pageNumber == endPageNumber) {
                        "Page $pageNumber"
                    } else {
                        "Pages $pageNumber-$endPageNumber"
                    },
                    content = pdfDoc.text,
                    anchor = "#page-$pageNumber",
                    page = pageNumber,
                    position = PositionMetadata(
                        documentPath = file.path,
                        formatType = DocumentFormatType.PDF,
                        position = DocumentPosition.PageRange(pageNumber, endPageNumber)
                    )
                )
            }
            logger.info { "Created ${currentChunks.size} document chunks" }

            // Extract TOC
            Loader.loadPDF(pdfFile).use { document ->
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
