package cc.unitmesh.harmonyos.actions.auto

data class AutoArkUiContext(
    val requirement: String,
    val layoutOverride: String,
    val componentOverride: String,
    var elements: List<String> = emptyList(),
)
