package cc.unitmesh.devti.gui.block

import cc.unitmesh.devti.gui.chat.ChatRole

interface CompletableMessage {
    val text: String
    val displayText: String

    fun getRole(): ChatRole

    fun addTextListener(textListener: MessageBlockTextListener)
    fun removeTextListener(textListener: MessageBlockTextListener)
}

