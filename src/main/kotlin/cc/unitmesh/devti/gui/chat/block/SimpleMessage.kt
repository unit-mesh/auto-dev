package cc.unitmesh.devti.gui.chat.block

import cc.unitmesh.devti.gui.chat.ChatRole

class SimpleMessage(
    override val displayText: String,
    override val text: String,
    override val role: ChatRole
) : CompletableMessage {
    private val textListeners: MutableList<MessageBlockTextListener> = mutableListOf()

    override fun addTextListener(textListener: MessageBlockTextListener) {
        textListeners += textListener
    }

    override fun removeTextListener(textListener: MessageBlockTextListener) {
        textListeners -= textListener
    }
}