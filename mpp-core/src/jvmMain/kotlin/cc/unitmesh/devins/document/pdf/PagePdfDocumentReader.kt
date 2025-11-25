/*
 * Copyright 2023-2024 the original author or authors.
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

import cc.unitmesh.devins.document.pdf.config.PdfDocumentReaderConfig
import cc.unitmesh.devins.document.pdf.layout.PDFLayoutTextStripperByArea
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import java.awt.Rectangle
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * Groups the parsed PDF pages into Documents. You can group one or more pages
 * into a single output document. Use [PdfDocumentReaderConfig] for customization
 * options. The default configuration is: - pagesPerDocument = 1 - pageTopMargin = 0 -
 * pageBottomMargin = 0
 *
 * @author Christian Tzolov
 * @author Fu Jian
 */
class PagePdfDocumentReader {
    companion object {
        const val METADATA_START_PAGE_NUMBER = "page_number"
        const val METADATA_END_PAGE_NUMBER = "end_page_number"
        const val METADATA_FILE_NAME = "file_name"
        private const val PDF_PAGE_REGION = "pdfPageRegion"
    }

    protected val document: PDDocument
    protected var resourceFileName: String
    private var config: PdfDocumentReaderConfig

    constructor(resourcePath: String) : this(resourcePath, PdfDocumentReaderConfig.defaultConfig())

    constructor(resourcePath: String, config: PdfDocumentReaderConfig) {
        try {
            val file = File(resourcePath)
            this.document = Loader.loadPDF(file)
            this.resourceFileName = file.name
            this.config = config
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun get(): List<PdfDocument> {
        val readDocuments = mutableListOf<PdfDocument>()
        try {
            val pdfTextStripper = PDFLayoutTextStripperByArea()

            var pageNumber = 1
            var startPageNumber = 1

            val pageTextGroupList = mutableListOf<String>()

            val pages = document.documentCatalog.pages
            val totalPages = pages.count
            val logFrequency = if (totalPages > 10) totalPages / 10 else 1

            val pagesPerDocument = getPagesPerDocument(totalPages)
            for (page in pages) {
                if ((pageNumber - 1) % logFrequency == 0) {
                    logger.info { "Processing PDF page: $pageNumber" }
                }

                handleSinglePage(page, pageNumber, pdfTextStripper, pageTextGroupList)

                if (pageNumber % pagesPerDocument == 0 || pageNumber == totalPages) {
                    if (pageTextGroupList.isNotEmpty()) {
                        readDocuments.add(
                            toDocument(
                                pageTextGroupList.joinToString(""),
                                startPageNumber,
                                pageNumber
                            )
                        )
                        pageTextGroupList.clear()
                    }
                    startPageNumber = pageNumber + 1
                }

                pageNumber++
            }

            logger.info { "Processed total $totalPages pages" }
            return readDocuments
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    private fun handleSinglePage(
        page: PDPage,
        pageNumber: Int,
        pdfTextStripper: PDFLayoutTextStripperByArea,
        pageTextGroupList: MutableList<String>
    ) {
        val x0 = page.mediaBox.lowerLeftX.toInt()
        val xW = page.mediaBox.width.toInt()

        val y0 = page.mediaBox.lowerLeftY.toInt() + config.pageTopMargin
        val yW = page.mediaBox.height.toInt() - (config.pageTopMargin + config.pageBottomMargin)

        pdfTextStripper.addRegion(PDF_PAGE_REGION, Rectangle(x0, y0, xW, yW))
        pdfTextStripper.extractRegions(page)
        var pageText = pdfTextStripper.getTextForRegion(PDF_PAGE_REGION)

        if (pageText.isNotBlank()) {
            pageText = config.pageExtractedTextFormatter(pageText, pageNumber)
            pageTextGroupList.add(pageText)
        }
        pdfTextStripper.removeRegion(PDF_PAGE_REGION)
    }

    private fun getPagesPerDocument(totalPages: Int): Int {
        if (config.pagesPerDocument == PdfDocumentReaderConfig.ALL_PAGES) {
            return totalPages
        }
        return config.pagesPerDocument
    }

    protected fun toDocument(docText: String, startPageNumber: Int, endPageNumber: Int): PdfDocument {
        val metadata = mutableMapOf<String, Any>()
        metadata[METADATA_START_PAGE_NUMBER] = startPageNumber
        if (startPageNumber != endPageNumber) {
            metadata[METADATA_END_PAGE_NUMBER] = endPageNumber
        }
        metadata[METADATA_FILE_NAME] = resourceFileName
        return PdfDocument(docText, metadata)
    }
}

/**
 * Represents a document extracted from PDF
 */
data class PdfDocument(
    val text: String,
    val metadata: Map<String, Any> = emptyMap()
)
