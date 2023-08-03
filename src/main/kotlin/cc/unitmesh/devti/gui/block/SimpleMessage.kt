package cc.unitmesh.devti.gui.block

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.gui.chat.message.ChatMessageRating

class SimpleMessage(
    override val displayText: String,
    override val text: String,
    val chatRole: ChatRole,
    override var rating: ChatMessageRating = ChatMessageRating.None
) : CompletableMessage {
    private val textListeners: MutableList<MessageBlockTextListener> = mutableListOf()
    override fun getRole(): ChatRole = chatRole

    override fun addTextListener(textListener: MessageBlockTextListener) {
        textListeners += textListener
    }

    override fun removeTextListener(textListener: MessageBlockTextListener) {
        textListeners -= textListener
    }
}