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

internal class TextLine(lineLength: Int) {
    private val lineLength: Int
    private val line: CharArray
    private var lastIndex: Int = 0

    init {
        require(lineLength >= 0) { "Line length cannot be negative" }
        this.lineLength = lineLength / ForkPDFLayoutTextStripper.OUTPUT_SPACE_CHARACTER_WIDTH_IN_PT
        this.line = CharArray(this.lineLength) { SPACE_CHARACTER }
    }

    fun writeCharacterAtIndex(character: Character) {
        character.index = computeIndexForCharacter(character)
        val index = character.index
        val characterValue = character.characterValue
        if (indexIsInBounds(index) && line[index] == SPACE_CHARACTER) {
            line[index] = characterValue
        }
    }

    fun getLineLength(): Int = lineLength

    fun getLine(): String = String(line)

    private fun computeIndexForCharacter(character: Character): Int {
        var index = character.index
        val isCharacterPartOfPreviousWord = character.isCharacterPartOfPreviousWord
        val isCharacterAtTheBeginningOfNewLine = character.isCharacterAtTheBeginningOfNewLine
        val isCharacterCloseToPreviousWord = character.isCharacterCloseToPreviousWord

        if (!indexIsInBounds(index)) {
            return -1
        } else {
            if (isCharacterPartOfPreviousWord && !isCharacterAtTheBeginningOfNewLine) {
                index = findMinimumIndexWithSpaceCharacterFromIndex(index)
            } else if (isCharacterCloseToPreviousWord) {
                if (line[index] != SPACE_CHARACTER) {
                    index = index + 1
                } else {
                    index = findMinimumIndexWithSpaceCharacterFromIndex(index) + 1
                }
            }
            index = getNextValidIndex(index, isCharacterPartOfPreviousWord)
            return index
        }
    }

    private fun isNotSpaceCharacterAtIndex(index: Int): Boolean {
        return line[index] != SPACE_CHARACTER
    }

    private fun isNewIndexGreaterThanLastIndex(index: Int): Boolean {
        return index > lastIndex
    }

    private fun getNextValidIndex(index: Int, isCharacterPartOfPreviousWord: Boolean): Int {
        var nextValidIndex = index
        if (!isNewIndexGreaterThanLastIndex(index)) {
            nextValidIndex = lastIndex + 1
        }
        if (!isCharacterPartOfPreviousWord && index > 0 && isNotSpaceCharacterAtIndex(index - 1)) {
            nextValidIndex = nextValidIndex + 1
        }
        lastIndex = nextValidIndex
        return nextValidIndex
    }

    private fun findMinimumIndexWithSpaceCharacterFromIndex(index: Int): Int {
        var newIndex = index
        while (newIndex >= 0 && line[newIndex] == SPACE_CHARACTER) {
            newIndex = newIndex - 1
        }
        return newIndex + 1
    }

    private fun indexIsInBounds(index: Int): Boolean {
        return index >= 0 && index < lineLength
    }

    companion object {
        private const val SPACE_CHARACTER = ' '
    }
}
