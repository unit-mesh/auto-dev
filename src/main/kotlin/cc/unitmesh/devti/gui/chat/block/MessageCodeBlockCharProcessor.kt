package cc.unitmesh.devti.gui.chat.block

enum class BorderType {
    START,
    END
}

class Parameters(val char: Char, val charIndex: Int, val fullMessage: String)
class ContextChange(@JvmField val contextType: BlockType, @JvmField val borderType: BorderType)

class MessageCodeBlockCharProcessor {
    companion object {
        const val triggerChar: Char = '`'
        const val borderBlock: String = "```"
    }

    fun suggestTypeChange(
        parameters: Parameters,
        currentContextType: BlockType,
        blockStart: Int
    ): ContextChange? {
        if (parameters.char != triggerChar && parameters.char != '\n') return null

        return when (currentContextType) {
            BlockType.PlainText -> {
                if (isCodeBlockStart(parameters)) {
                    ContextChange(BlockType.CodeEditor, BorderType.START)
                } else {
                    null
                }
            }

            BlockType.CodeEditor -> {
                if (isCodeBlockEnd(parameters, blockStart)) {
                    ContextChange(BlockType.CodeEditor, BorderType.END)
                } else {
                    null
                }
            }
        }
    }

    fun isCodeBlockEnd(parameters: Parameters, blockStart: Int): Boolean {
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

    fun isCodeBlockStart(parameters: Parameters): Boolean {
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