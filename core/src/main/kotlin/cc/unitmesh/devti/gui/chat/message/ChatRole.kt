package cc.unitmesh.devti.gui.chat.message

enum class ChatRole {
    System,
    Assistant,
    User;

    fun roleName(): String {
        return this.name.lowercase()
    }
}