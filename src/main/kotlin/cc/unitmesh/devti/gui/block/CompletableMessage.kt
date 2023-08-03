package cc.unitmesh.devti.gui.block

import cc.unitmesh.devti.gui.chat.ChatRole
import cc.unitmesh.devti.gui.chat.message.ChatMessageRating
import com.intellij.openapi.actionSystem.DataKey

interface CompletableMessage {
    val text: String
    val displayText: String
    var rating: ChatMessageRating

    fun getRole(): ChatRole

    fun addTextListener(textListener: MessageBlockTextListener)
    fun removeTextListener(textListener: MessageBlockTextListener)

    companion object {
        val key: DataKey<CompletableMessage> = DataKey.create("CompletableMessage")

    }
}

