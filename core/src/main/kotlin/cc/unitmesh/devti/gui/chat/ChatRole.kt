package cc.unitmesh.devti.gui.chat

enum class ChatRole {
    System,
    Assistant,
    User;

    fun roleName(): String {
        return this.name.lowercase()
    }
}