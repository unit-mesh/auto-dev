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

internal class Character(
    val characterValue: Char,
    var index: Int,
    val isCharacterPartOfPreviousWord: Boolean,
    val isFirstCharacterOfAWord: Boolean,
    val isCharacterAtTheBeginningOfNewLine: Boolean,
    val isCharacterCloseToPreviousWord: Boolean
) {
    init {
        if (ForkPDFLayoutTextStripper.DEBUG) {
            println(toString())
        }
    }

    override fun toString(): String {
        return buildString {
            append(index)
            append(" ")
            append(characterValue)
            append(" isCharacterPartOfPreviousWord=$isCharacterPartOfPreviousWord")
            append(" isFirstCharacterOfAWord=$isFirstCharacterOfAWord")
            append(" isCharacterAtTheBeginningOfNewLine=$isCharacterAtTheBeginningOfNewLine")
            append(" isCharacterPartOfASentence=$isCharacterCloseToPreviousWord")
            append(" isCharacterCloseToPreviousWord=$isCharacterCloseToPreviousWord")
        }
    }
}
