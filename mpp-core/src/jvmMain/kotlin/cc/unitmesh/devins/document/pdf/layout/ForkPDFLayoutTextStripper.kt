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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition
import org.apache.pdfbox.text.TextPositionComparator
import java.io.IOException
import kotlin.math.floor
import kotlin.math.round

private val logger = KotlinLogging.logger {}

/**
 * This class extends PDFTextStripper to provide custom text extraction and formatting
 * capabilities for PDF pages. It includes features like processing text lines, sorting
 * text positions, and managing line breaks.
 *
 * @author Jonathan Link
 */
open class ForkPDFLayoutTextStripper : PDFTextStripper() {

    private var currentPageWidth: Double = 0.0
    private var previousTextPosition: TextPosition? = null
    private var textLineList: MutableList<TextLine> = mutableListOf()

    init {
        this.previousTextPosition = null
        this.textLineList = mutableListOf()
    }

    @Throws(IOException::class)
    override fun processPage(page: PDPage) {
        val pageRectangle = page.mediaBox
        if (pageRectangle != null) {
            setCurrentPageWidth(pageRectangle.width * 1.4)
            super.processPage(page)
            this.previousTextPosition = null
            this.textLineList = mutableListOf()
        }
    }

    @Throws(IOException::class)
    override fun writePage() {
        val charactersByArticle = getCharactersByArticle()
        for (textList in charactersByArticle) {
            try {
                sortTextPositionList(textList)
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Error sorting text positions" }
            }
            iterateThroughTextList(textList.iterator())
        }
        writeToOutputStream(getTextLineList())
    }

    @Throws(IOException::class)
    private fun writeToOutputStream(textLineList: List<TextLine>) {
        for (textLine in textLineList) {
            val line = textLine.getLine().toCharArray()
            output.write(line)
            output.write('\n'.code)
            output.flush()
        }
    }

    /*
     * In order to get rid of the warning: TextPositionComparator class should implement
     * Comparator<TextPosition> instead of Comparator
     */
    private fun sortTextPositionList(textList: MutableList<TextPosition>) {
        val comparator = TextPositionComparator()
        textList.sortWith(comparator)
    }

    private fun writeLine(textPositionList: List<TextPosition>) {
        if (textPositionList.isNotEmpty()) {
            val textLine = addNewLine()
            var firstCharacterOfLineFound = false
            for (textPosition in textPositionList) {
                val characterFactory = CharacterFactory(firstCharacterOfLineFound)
                val character = characterFactory.createCharacterFromTextPosition(
                    textPosition,
                    getPreviousTextPosition()
                )
                textLine.writeCharacterAtIndex(character)
                setPreviousTextPosition(textPosition)
                firstCharacterOfLineFound = true
            }
        } else {
            addNewLine() // white line
        }
    }

    private fun iterateThroughTextList(textIterator: Iterator<TextPosition>) {
        val textPositionList = mutableListOf<TextPosition>()

        while (textIterator.hasNext()) {
            val textPosition = textIterator.next()
            val numberOfNewLines = getNumberOfNewLinesFromPreviousTextPosition(textPosition)
            if (numberOfNewLines == 0) {
                textPositionList.add(textPosition)
            } else {
                writeTextPositionList(textPositionList)
                createNewEmptyNewLines(numberOfNewLines)
                textPositionList.add(textPosition)
            }
            setPreviousTextPosition(textPosition)
        }
        if (textPositionList.isNotEmpty()) {
            writeTextPositionList(textPositionList)
        }
    }

    private fun writeTextPositionList(textPositionList: MutableList<TextPosition>) {
        writeLine(textPositionList)
        textPositionList.clear()
    }

    private fun createNewEmptyNewLines(numberOfNewLines: Int) {
        for (i in 0 until numberOfNewLines - 1) {
            addNewLine()
        }
    }

    private fun getNumberOfNewLinesFromPreviousTextPosition(textPosition: TextPosition): Int {
        val previousTextPosition = getPreviousTextPosition()
        if (previousTextPosition == null) {
            return 1
        }

        val textYPosition = round(textPosition.y)
        val previousTextYPosition = round(previousTextPosition.y)

        if (textYPosition > previousTextYPosition && (textYPosition - previousTextYPosition > 5.5)) {
            val height = textPosition.height.toDouble()
            var numberOfLines = (floor(textYPosition - previousTextYPosition) / height).toInt()
            numberOfLines = maxOf(1, numberOfLines - 1) // exclude current new line
            if (DEBUG) {
                println("$height $numberOfLines")
            }
            return numberOfLines
        } else {
            return 0
        }
    }

    private fun addNewLine(): TextLine {
        val textLine = TextLine(getCurrentPageWidth())
        this.textLineList.add(textLine)
        return textLine
    }

    private fun getPreviousTextPosition(): TextPosition? {
        return this.previousTextPosition
    }

    private fun setPreviousTextPosition(setPreviousTextPosition: TextPosition) {
        this.previousTextPosition = setPreviousTextPosition
    }

    private fun getCurrentPageWidth(): Int {
        return round(this.currentPageWidth).toInt()
    }

    private fun setCurrentPageWidth(currentPageWidth: Double) {
        this.currentPageWidth = currentPageWidth
    }

    private fun getTextLineList(): List<TextLine> {
        return this.textLineList
    }

    companion object {
        const val DEBUG = false
        const val OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT = 4
    }
}
