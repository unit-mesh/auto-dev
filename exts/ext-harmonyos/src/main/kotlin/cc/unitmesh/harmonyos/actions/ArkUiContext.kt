package cc.unitmesh.harmonyos.actions

data class ArkUiContext(
    val selectedText: String,
    val layoutType: List<LayoutType> = emptyList(),
    val componentType: List<ComponentType> = emptyList(),
)
