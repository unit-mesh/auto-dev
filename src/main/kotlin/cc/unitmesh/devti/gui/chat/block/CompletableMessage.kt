package cc.unitmesh.devti.gui.chat.block

import cc.unitmesh.devti.gui.chat.ChatRole

interface CompletableMessage {
    val text: String
    val displayText: String
    val role: ChatRole

    fun addContent(addedContent: String)
    fun replaceContent(content: String)
    fun addTextListener(textListener: MessageBlockTextListener)
    fun removeTextListener(textListener: MessageBlockTextListener)
}

interface MessageBlockTextListener {
    fun onTextChanged(str: String)
}
