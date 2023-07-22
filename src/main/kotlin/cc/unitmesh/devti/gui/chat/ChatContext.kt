package cc.unitmesh.devti.gui.chat

data class ChatContext(
    val replaceSelectedText: ((response: String) -> Unit)? = null,
    val prefixText: String,
    val suffixText: String
)