package cc.unitmesh.devti.editor.inlay

enum class InlayDisposeContext {
    UserAction,
    IdeCompletion,
    CaretChange,
    SelectionChange,
    SettingsChange,
    Cycling,
    TypingAsSuggested,
    Typing,
    Applied,
    ;

    val isResetLastRequest: Boolean
        get() = this == SettingsChange || this == Applied
    val isSendRejectedTelemetry: Boolean
        get() = this == UserAction
}
