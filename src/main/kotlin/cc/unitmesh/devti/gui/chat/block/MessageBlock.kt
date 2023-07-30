package cc.unitmesh.devti.gui.chat.block

interface MessageBlock {
    val type: MessageBlockType
    val message: CompletableMessage
    val textContent: String

    fun addContent(blockText: String)
}

class TextBlock(override val message: CompletableMessage) : MessageBlock {
    override val type: MessageBlockType = MessageBlockType.PlainText
    override val textContent: String
        get() = message.text

    override fun addContent(blockText: String) {

    }
}

class CodeBlock(override val message: CompletableMessage) : MessageBlock {
    override val type: MessageBlockType = MessageBlockType.CodeEditor
    override val textContent: String
        get() = message.text

    override fun addContent(blockText: String) {

    }
}