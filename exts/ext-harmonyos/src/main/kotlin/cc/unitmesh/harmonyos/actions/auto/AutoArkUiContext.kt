package cc.unitmesh.harmonyos.actions.auto

data class AutoArkUiContext(
    val requirement: String,
    val layoutOverride: String,
    val componentOverride: String,
    val layouts: List<String> = emptyList(),
    val components: List<String> = emptyList(),
)
