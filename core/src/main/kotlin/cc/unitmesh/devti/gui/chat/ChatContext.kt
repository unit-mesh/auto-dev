package cc.unitmesh.devti.gui.chat

data class ChatContext(
    val postAction: ((response: String) -> Unit)? = null,
    val prefixText: String,
    val suffixText: String
)