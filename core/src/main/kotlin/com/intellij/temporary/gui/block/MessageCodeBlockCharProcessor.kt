// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.temporary.gui.block

enum class BorderType {
    START,
    END
}

class Parameters(val char: Char, val charIndex: Int, val fullMessage: String)
class ContextChange(@JvmField val contextType: MessageBlockType, @JvmField val borderType: BorderType)

class MessageCodeBlockCharProcessor {
    private val triggerChar: Char = '`'
    private val borderBlock: String = "```"

    fun suggestTypeChange(
        parameters: Parameters,
        currentContextType: MessageBlockType,
        blockStart: Int
    ): ContextChange? {
        if (parameters.char != triggerChar && parameters.char != '\n') return null

        return when (currentContextType) {
            MessageBlockType.PlainText -> {
                if (isCodeBlockStart(parameters)) {
                    ContextChange(MessageBlockType.CodeEditor, BorderType.START)
                } else {
                    null
                }
            }

            MessageBlockType.CodeEditor -> {
                if (isCodeBlockEnd(parameters, blockStart)) {
                    ContextChange(MessageBlockType.CodeEditor, BorderType.END)
                } else {
                    null
                }
            }
        }
    }

    private fun isCodeBlockEnd(parameters: Parameters, blockStart: Int): Boolean {
        if (parameters.charIndex - blockStart < 5) {
            return false
        }
        val fullMessage = parameters.fullMessage
        val charIndex = parameters.charIndex
        return when {
            parameters.char == triggerChar && charIndex == fullMessage.length - 1 -> {
                val subSequence = fullMessage.subSequence(charIndex - 3, charIndex + 1)
                subSequence == "\n$borderBlock"
            }

            parameters.char == '\n' && (charIndex - 3) - 1 >= 0 -> {
                val subSequence = fullMessage.subSequence(charIndex - 4, charIndex)
                subSequence == "\n$borderBlock"
            }

            else -> false
        }
    }

    private fun isCodeBlockStart(parameters: Parameters): Boolean {
        if (parameters.char == triggerChar && parameters.charIndex + 3 < parameters.fullMessage.length) {
            val isLineStart = parameters.charIndex == 0 || parameters.fullMessage[parameters.charIndex - 1] == '\n'
            if (isLineStart) {
                val subSequence = parameters.fullMessage.subSequence(parameters.charIndex, parameters.charIndex + 3)
                return subSequence.all { it == triggerChar }
            }
        }
        return false
    }
}