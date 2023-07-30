package cc.unitmesh.devti.gui.chat.block

interface MessageBlock {
    val type: BlockType
    val message: CompletableMessage
    val textContent: String
}