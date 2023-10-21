package cc.unitmesh.devti.custom.team

import cc.unitmesh.cf.core.llms.LlmMsg

data class TeamActionPrompt(
    val interaction: InteractionType = InteractionType.AppendCursorStream,
    val msgs: List<LlmMsg.ChatMessage> = listOf(),
) {
    companion object {
        fun fromContent(promptContent: String) : TeamActionPrompt {
            TODO("Not yet implemented")
        }
    }
}

enum class InteractionType {
    ChatPanel,
    AppendCursor,
    AppendCursorStream,
    ;
}