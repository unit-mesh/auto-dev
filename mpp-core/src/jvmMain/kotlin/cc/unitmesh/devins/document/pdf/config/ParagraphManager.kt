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

package cc.unitmesh.devins.document.pdf.config

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import java.io.IOException
import java.io.PrintStream

/**
 * The ParagraphManager class is responsible for managing the paragraphs and hierarchy of
 * a PDF document. It can process bookmarks and generate a structured tree of paragraphs,
 * representing the table of contents (TOC) of the PDF document.
 *
 * @author Christian Tzolov
 */
class ParagraphManager(private val document: PDDocument) {

    /**
     * Root of the paragraphs tree.
     */
    private val rootParagraph: Paragraph

    init {
        requireNotNull(document) { "PDDocument must not be null" }
        requireNotNull(document.documentCatalog.documentOutline) {
            "Document outline (e.g. TOC) is null. " +
                    "Make sure the PDF document has a table of contents (TOC). If not, consider the " +
                    "PagePdfDocumentReader or the TikaDocumentReader instead."
        }

        try {
            this.rootParagraph = generateParagraphs(
                Paragraph(null, "root", -1, 1, document.numberOfPages, 0),
                document.documentCatalog.documentOutline,
                0
            )

            printParagraph(this.rootParagraph, System.out)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun flatten(): List<Paragraph> {
        val paragraphs = mutableListOf<Paragraph>()
        for (child in rootParagraph.children) {
            flatten(child, paragraphs)
        }
        return paragraphs
    }

    private fun flatten(current: Paragraph, paragraphs: MutableList<Paragraph>) {
        paragraphs.add(current)
        for (child in current.children) {
            flatten(child, paragraphs)
        }
    }

    private fun printParagraph(paragraph: Paragraph, printStream: PrintStream) {
        printStream.println(paragraph)
        for (childParagraph in paragraph.children) {
            printParagraph(childParagraph, printStream)
        }
    }

    /**
     * For given [PDOutlineNode] bookmark convert all sibling [PDOutlineItem]
     * items into [Paragraph] instances under the parentParagraph. For each
     * [PDOutlineItem] item, recursively call
     * [ParagraphManager.generateParagraphs] to process its children items.
     * @param parentParagraph Root paragraph that the bookmark sibling items should be
     * added to.
     * @param bookmark TOC paragraphs to process.
     * @param level Current TOC deepness level.
     * @return Returns a tree of [Paragraph]s that represent the PDF document TOC.
     * @throws IOException
     */
    @Throws(IOException::class)
    protected fun generateParagraphs(
        parentParagraph: Paragraph,
        bookmark: PDOutlineNode,
        level: Int
    ): Paragraph {
        var current = bookmark.firstChild

        while (current != null) {
            val pageNumber = getPageNumber(current)
            var nextSiblingNumber = getPageNumber(current.nextSibling)
            if (nextSiblingNumber < 0) {
                nextSiblingNumber = getPageNumber(current.lastChild)
            }

            val paragraphPosition = if (current.destination is PDPageXYZDestination) {
                (current.destination as PDPageXYZDestination).top
            } else {
                0
            }

            val currentParagraph = Paragraph(
                parentParagraph,
                current.title,
                level,
                pageNumber,
                nextSiblingNumber,
                paragraphPosition
            )

            parentParagraph.children.add(currentParagraph)

            // Recursive call to go the current paragraph's children paragraphs.
            // E.g. go one level deeper.
            generateParagraphs(currentParagraph, current, level + 1)

            current = current.nextSibling
        }
        return parentParagraph
    }

    @Throws(IOException::class)
    private fun getPageNumber(current: PDOutlineItem?): Int {
        if (current == null) {
            return -1
        }
        val currentPage = current.findDestinationPage(document)
        if (currentPage != null) {
            val pages = document.documentCatalog.pages
            for (i in 0 until pages.count) {
                val page = pages[i]
                if (page == currentPage) {
                    return i + 1
                }
            }
        }
        return -1
    }

    fun getParagraphsByLevel(paragraph: Paragraph, level: Int, interLevelText: Boolean): List<Paragraph> {
        val resultList = mutableListOf<Paragraph>()

        if (paragraph.level < level) {
            if (paragraph.children.isNotEmpty()) {
                if (interLevelText) {
                    val interLevelParagraph = Paragraph(
                        paragraph.parent,
                        paragraph.title,
                        paragraph.level,
                        paragraph.startPageNumber,
                        paragraph.children[0].startPageNumber,
                        paragraph.position
                    )
                    resultList.add(interLevelParagraph)
                }

                for (child in paragraph.children) {
                    resultList.addAll(getParagraphsByLevel(child, level, interLevelText))
                }
            }
        } else if (paragraph.level == level) {
            resultList.add(paragraph)
        }

        return resultList
    }

    /**
     * Represents a document paragraph metadata and hierarchy.
     *
     * @param parent Parent paragraph that will contain a children paragraphs.
     * @param title Paragraph title as it appears in the PDF document.
     * @param level The TOC deepness level for this paragraph. The root is at level 0.
     * @param startPageNumber The page number in the PDF where this paragraph begins.
     * @param endPageNumber The page number in the PDF where this paragraph ends.
     * @param position The vertical position of the paragraph on the page.
     * @param children Sub-paragraphs for this paragraph.
     */
    data class Paragraph(
        val parent: Paragraph?,
        val title: String,
        val level: Int,
        val startPageNumber: Int,
        val endPageNumber: Int,
        val position: Int,
        val children: MutableList<Paragraph> = mutableListOf()
    ) {
        constructor(
            parent: Paragraph?,
            title: String,
            level: Int,
            startPageNumber: Int,
            endPageNumber: Int,
            position: Int
        ) : this(parent, title, level, startPageNumber, endPageNumber, position, mutableListOf())

        override fun toString(): String {
            val indent = if (level < 0) "" else " ".repeat(level * 2)
            return "$indent $level) $title [$startPageNumber,$endPageNumber], children = ${children.size}, pos = $position"
        }
    }
}
