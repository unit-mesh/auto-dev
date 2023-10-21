package cc.unitmesh.devti.custom.team

class TeamCustomConfig(
    val interaction: InteractionType,
) {
}

enum class InteractionType {
    ChatPanel,
    AppendCursor,
    AppendCursorStream,
    ;
}