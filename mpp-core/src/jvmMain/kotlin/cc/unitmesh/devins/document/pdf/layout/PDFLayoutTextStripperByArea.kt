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

package cc.unitmesh.devins.document.pdf.layout

import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.TextPosition
import java.awt.geom.Rectangle2D
import java.io.IOException
import java.io.StringWriter

/**
 * Re-implement the PDFLayoutTextStripperByArea on top of the PDFLayoutTextStripper
 * instead the original PDFTextStripper.
 *
 * This class allows cropping pages (e.g., removing headers, footers, and between-page
 * empty spaces) while extracting layout text, preserving the PDF's internal text
 * formatting.
 *
 * @author Christian Tzolov
 */
class PDFLayoutTextStripperByArea : ForkPDFLayoutTextStripper() {

    private val regions = mutableListOf<String>()
    private val regionArea = mutableMapOf<String, Rectangle2D>()
    private val regionCharacterList = mutableMapOf<String, ArrayList<MutableList<TextPosition>>>()
    private val regionText = mutableMapOf<String, StringWriter>()

    /**
     * Constructor.
     * @throws IOException If there is an error loading properties.
     */
    init {
        super.setShouldSeparateByBeads(false)
    }

    /**
     * This method does nothing in this derived class, because beads and regions are
     * incompatible. Beads are ignored when stripping by area.
     * @param aShouldSeparateByBeads The new grouping of beads.
     */
    override fun setShouldSeparateByBeads(aShouldSeparateByBeads: Boolean) {
        // Do nothing - beads are incompatible with regions
    }

    /**
     * Add a new region to group text by.
     * @param regionName The name of the region.
     * @param rect The rectangle area to retrieve the text from. The y-coordinates are
     * java coordinates (y == 0 is top), not PDF coordinates (y == 0 is bottom).
     */
    fun addRegion(regionName: String, rect: Rectangle2D) {
        regions.add(regionName)
        regionArea[regionName] = rect
    }

    /**
     * Delete a region to group text by. If the region does not exist, this method does
     * nothing.
     * @param regionName The name of the region to delete.
     */
    fun removeRegion(regionName: String) {
        regions.remove(regionName)
        regionArea.remove(regionName)
    }

    /**
     * Get the list of regions that have been setup.
     * @return A list of java.lang.String objects to identify the region names.
     */
    fun getRegions(): List<String> {
        return regions
    }

    /**
     * Get the text for the region, this should be called after extractRegions().
     * @param regionName The name of the region to get the text from.
     * @return The text that was identified in that region.
     */
    fun getTextForRegion(regionName: String): String {
        val text = regionText[regionName]
        return text?.toString() ?: ""
    }

    /**
     * Process the page to extract the region text.
     * @param page The page to extract the regions from.
     * @throws IOException If there is an error while extracting text.
     */
    @Throws(IOException::class)
    fun extractRegions(page: PDPage) {
        for (regionName in regions) {
            startPage = currentPageNo
            endPage = currentPageNo
            // reset the stored text for the region so this class can be reused.
            val regionCharactersByArticle = ArrayList<MutableList<TextPosition>>()
            regionCharactersByArticle.add(mutableListOf())
            regionCharacterList[regionName] = regionCharactersByArticle
            regionText[regionName] = StringWriter()
        }

        if (page.hasContents()) {
            processPage(page)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(IOException::class)
    override fun processTextPosition(text: TextPosition) {
        for ((regionName, rect) in regionArea) {
            if (rect.contains(text.x.toDouble(), text.y.toDouble())) {
                charactersByArticle = regionCharacterList[regionName]
                super.processTextPosition(text)
            }
        }
    }

    /**
     * This will print the processed page text to the output stream.
     * @throws IOException If there is an error writing the text.
     */
    @Throws(IOException::class)
    override fun writePage() {
        for (region in regionArea.keys) {
            charactersByArticle = regionCharacterList[region]
            output = regionText[region]
            super.writePage()
        }
    }
}
