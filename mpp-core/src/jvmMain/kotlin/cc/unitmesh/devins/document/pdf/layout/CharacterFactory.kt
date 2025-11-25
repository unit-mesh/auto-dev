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

package cc.unitmesh.devins.document.pdf.layout

import org.apache.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.round

internal class CharacterFactory(
    private val firstCharacterOfLineFound: Boolean
) {
    private var previousTextPosition: TextPosition? = null
    private var isCharacterPartOfPreviousWord: Boolean = false
    private var isFirstCharacterOfAWord: Boolean = false
    private var isCharacterAtTheBeginningOfNewLine: Boolean = false
    private var isCharacterCloseToPreviousWord: Boolean = false

    fun createCharacterFromTextPosition(
        textPosition: TextPosition,
        previousTextPosition: TextPosition?
    ): Character {
        this.previousTextPosition = previousTextPosition
        this.isCharacterPartOfPreviousWord = isCharacterPartOfPreviousWord(textPosition)
        this.isFirstCharacterOfAWord = isFirstCharacterOfAWord(textPosition)
        this.isCharacterAtTheBeginningOfNewLine = isCharacterAtTheBeginningOfNewLine(textPosition)
        this.isCharacterCloseToPreviousWord = isCharacterCloseToPreviousWord(textPosition)
        val character = getCharacterFromTextPosition(textPosition)
        val index = (textPosition.x / ForkPDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT).toInt()
        return Character(
            character,
            index,
            this.isCharacterPartOfPreviousWord,
            this.isFirstCharacterOfAWord,
            this.isCharacterAtTheBeginningOfNewLine,
            this.isCharacterCloseToPreviousWord
        )
    }

    private fun isCharacterAtTheBeginningOfNewLine(textPosition: TextPosition): Boolean {
        if (!firstCharacterOfLineFound) {
            return true
        }
        val previousTextPosition = this.previousTextPosition ?: return true
        val previousTextYPosition = previousTextPosition.y
        return round(textPosition.y) < round(previousTextYPosition)
    }

    private fun isFirstCharacterOfAWord(textPosition: TextPosition): Boolean {
        if (!firstCharacterOfLineFound) {
            return true
        }
        val numberOfSpaces = numberOfSpacesBetweenTwoCharacters(this.previousTextPosition!!, textPosition)
        return (numberOfSpaces > 1) || isCharacterAtTheBeginningOfNewLine(textPosition)
    }

    private fun isCharacterCloseToPreviousWord(textPosition: TextPosition): Boolean {
        if (!firstCharacterOfLineFound) {
            return false
        }
        val numberOfSpaces = numberOfSpacesBetweenTwoCharacters(this.previousTextPosition!!, textPosition)
        return numberOfSpaces > 1 && numberOfSpaces <= ForkPDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT
    }

    private fun isCharacterPartOfPreviousWord(textPosition: TextPosition): Boolean {
        val previousTextPosition = this.previousTextPosition ?: return false
        if (previousTextPosition.unicode == " ") {
            return false
        }
        val numberOfSpaces = numberOfSpacesBetweenTwoCharacters(previousTextPosition, textPosition)
        return numberOfSpaces <= 1
    }

    private fun numberOfSpacesBetweenTwoCharacters(
        textPosition1: TextPosition,
        textPosition2: TextPosition
    ): Double {
        val previousTextXPosition = textPosition1.x.toDouble()
        val previousTextWidth = textPosition1.width.toDouble()
        val previousTextEndXPosition = previousTextXPosition + previousTextWidth
        return abs(round(textPosition2.x - previousTextEndXPosition))
    }

    private fun getCharacterFromTextPosition(textPosition: TextPosition): Char {
        val string = textPosition.unicode
        return if (string.isNotEmpty()) string[0] else '\u0000'
    }
}
