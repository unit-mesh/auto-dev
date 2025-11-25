/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.unitmesh.devins.document.pdf

import cc.unitmesh.devins.document.pdf.config.ParagraphManager
import cc.unitmesh.devins.document.pdf.config.PdfDocumentReaderConfig
import cc.unitmesh.devins.document.pdf.layout.PDFLayoutTextStripperByArea
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import java.awt.Rectangle
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Uses the PDF catalog (e.g. TOC) information to split the input PDF into text paragraphs
 * and output a single [PdfDocument] per paragraph.
 *
 * This class provides methods for reading and processing PDF documents. It uses the
 * Apache PDFBox library for parsing PDF content and converting it into text paragraphs.
 * The paragraphs are grouped into [PdfDocument] objects.
 *
 * @author Christian Tzolov
 * @author Heonwoo Kim
 */
class ParagraphPdfDocumentReader {
    companion object {
        // Constants for metadata keys
        private const val METADATA_START_PAGE = "page_number"
        private const val METADATA_END_PAGE = "end_page_number"
        private const val METADATA_TITLE = "title"
        private const val METADATA_LEVEL = "level"
        private const val METADATA_FILE_NAME = "file_name"
    }

    protected val document: PDDocument
    private val paragraphTextExtractor: ParagraphManager
    protected var resourceFileName: String
    private var config: PdfDocumentReaderConfig

    /**
     * Constructs a ParagraphPdfDocumentReader using a resource path.
     * @param resourcePath The path of the PDF resource.
     */
    constructor(resourcePath: String) : this(resourcePath, PdfDocumentReaderConfig.defaultConfig())

    /**
     * Constructs a ParagraphPdfDocumentReader using a resource path and a configuration.
     * @param resourcePath The path of the PDF resource.
     * @param config The configuration for PDF document processing.
     */
    constructor(resourcePath: String, config: PdfDocumentReaderConfig) {
        try {
            val file = File(resourcePath)
            this.document = Loader.loadPDF(file)
            this.config = config
            this.paragraphTextExtractor = ParagraphManager(this.document)
            this.resourceFileName = file.name
        } catch (iae: IllegalArgumentException) {
            throw iae
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Reads and processes the PDF document to extract paragraphs.
     * @return A list of [PdfDocument] objects representing paragraphs.
     */
    fun get(): List<PdfDocument> {
        val paragraphs = paragraphTextExtractor.flatten()
        val documents = mutableListOf<PdfDocument>()
        if (paragraphs.isEmpty()) {
            return documents
        }
        logger.info { "Start processing paragraphs from PDF" }
        for (i in paragraphs.indices) {
            val from = paragraphs[i]
            val to = if (i + 1 < paragraphs.size) paragraphs[i + 1] else from
            val document = toDocument(from, to)
            if (document != null && document.text.isNotBlank()) {
                documents.add(document)
            }
        }
        logger.info { "End processing paragraphs from PDF" }
        return documents
    }

    protected fun toDocument(
        from: ParagraphManager.Paragraph,
        to: ParagraphManager.Paragraph
    ): PdfDocument? {
        val docText = getTextBetweenParagraphs(from, to)

        if (docText.isBlank()) {
            return null
        }

        val metadata = mutableMapOf<String, Any>()
        addMetadata(from, to, metadata)

        return PdfDocument(docText, metadata)
    }

    protected fun addMetadata(
        from: ParagraphManager.Paragraph,
        to: ParagraphManager.Paragraph,
        metadata: MutableMap<String, Any>
    ) {
        metadata[METADATA_TITLE] = from.title
        metadata[METADATA_START_PAGE] = from.startPageNumber
        metadata[METADATA_END_PAGE] = from.endPageNumber
        metadata[METADATA_LEVEL] = from.level
        metadata[METADATA_FILE_NAME] = resourceFileName
    }

    fun getTextBetweenParagraphs(
        fromParagraph: ParagraphManager.Paragraph,
        toParagraph: ParagraphManager.Paragraph
    ): String {
        if (fromParagraph.startPageNumber < 1) {
            logger.warn {
                "Skipping paragraph titled '${fromParagraph.title}' because it has an invalid start page number: ${fromParagraph.startPageNumber}"
            }
            return ""
        }

        // Page started from index 0, while PDFBox getPage return them from index 1.
        var startPage = fromParagraph.startPageNumber - 1
        var endPage = toParagraph.startPageNumber - 1

        if (fromParagraph == toParagraph || endPage < startPage) {
            endPage = startPage
        }

        try {
            val sb = StringBuilder()

            val pdfTextStripper = PDFLayoutTextStripperByArea()
            pdfTextStripper.sortByPosition = true

            for (pageNumber in startPage..endPage) {
                val page = document.getPage(pageNumber)
                val pageHeight = page.mediaBox.height

                val fromPos = fromParagraph.position
                val toPos = if (fromParagraph != toParagraph) toParagraph.position else 0

                val x = page.mediaBox.lowerLeftX.toInt()
                val w = page.mediaBox.width.toInt()
                val y: Int
                var h: Int

                when {
                    pageNumber == startPage && pageNumber == endPage -> {
                        y = toPos
                        h = fromPos - toPos
                    }
                    pageNumber == startPage -> {
                        y = 0
                        h = fromPos
                    }
                    pageNumber == endPage -> {
                        y = toPos
                        h = pageHeight.toInt() - toPos
                    }
                    else -> {
                        y = 0
                        h = pageHeight.toInt()
                    }
                }

                if (h < 0) {
                    h = 0
                }

                pdfTextStripper.addRegion("pdfPageRegion", Rectangle(x, y, w, h))
                pdfTextStripper.extractRegions(page)
                val text = pdfTextStripper.getTextForRegion("pdfPageRegion")
                if (text.isNotBlank()) {
                    sb.append(text)
                }
                pdfTextStripper.removeRegion("pdfPageRegion")
            }

            var text = sb.toString()

            if (text.isNotBlank()) {
                text = config.pageExtractedTextFormatter(text, startPage)
            }

            return text
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
