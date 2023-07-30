package cc.unitmesh.devti.gui.chat.block

interface MessageBlock {
    val type: MessageBlockType
    val message: CompletableMessage
    val textContent: String
    fun addContent(blockText: String)
    fun replaceContent(content: String)
}

abstract class AbstractMessageBlock(override val message: CompletableMessage) : MessageBlock {
    private val contentBuilder: StringBuilder

    init {
        contentBuilder = StringBuilder()
    }

    override val textContent: String = contentBuilder.toString()

    override fun addContent(blockText: String) {
        contentBuilder.append(blockText)
    }

    override fun replaceContent(content: String) {
        contentBuilder.clear()
        contentBuilder.append(content)
    }
}

class TextBlock(override val message: CompletableMessage) : AbstractMessageBlock(message) {
    override val type: MessageBlockType = MessageBlockType.PlainText
    override val textContent: String
        get() = message.text
}

class CodeBlock(override val message: CompletableMessage) : AbstractMessageBlock(message) {
    override val type: MessageBlockType = MessageBlockType.CodeEditor
    override val textContent: String
        get() = message.text

}