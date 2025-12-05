package cc.unitmesh.devti.custom.team

enum class InteractionType(val description: String) {
    AppendCursor("Append content at the current cursor position"),
    AppendCursorStream("Append content at the current cursor position, stream output"),
    OutputFile("Output to a new file"),
    ReplaceSelection("Replace the currently selected content"),
    ReplaceCurrentFile("Replace the content of the current file"),
    InsertBeforeSelection("Insert content before the currently selected content"),
    RunPanel("Show Result in Run panel which is the bottom of the IDE"),
    ChatPanel("Show Result in Chat panel which is the right of the IDE"),
    OnPaste("Copy the content to the clipboard"),
    RightPanel("Show Result in Right panel which is the right of the IDE"),
    StreamDiff("Use streaming diff to show the result")
    ;

    companion object {
        fun from(interaction: String): InteractionType {
            return when (interaction.lowercase()) {
                AppendCursor.name.lowercase() -> InteractionType.AppendCursor
                AppendCursorStream.name.lowercase() -> InteractionType.AppendCursorStream
                OutputFile.name.lowercase() -> InteractionType.OutputFile
                ReplaceSelection.name.lowercase() -> InteractionType.ReplaceSelection
                ReplaceCurrentFile.name.lowercase() -> InteractionType.ReplaceCurrentFile
                InsertBeforeSelection.name.lowercase() -> InsertBeforeSelection
                RunPanel.name.lowercase() -> InteractionType.RunPanel
                OnPaste.name.lowercase() -> OnPaste
                RightPanel.name.lowercase() -> RightPanel
                StreamDiff.name.lowercase() -> StreamDiff
                else -> RunPanel
            }
        }
    }
}