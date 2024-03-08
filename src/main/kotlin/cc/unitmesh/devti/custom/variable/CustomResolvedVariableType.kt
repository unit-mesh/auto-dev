package cc.unitmesh.devti.custom.variable

enum class CustomResolvedVariableType(@JvmField val description: String) {
    SELECTION("Currently selected code fragment with language name"),
    METHOD_INPUT_OUTPUT("Method input parameter's class as code snippets"),
    SPEC_VARIABLE("Load from spec config, and config to items"),
    SIMILAR_CHUNK("Similar code chunk with element's code and recently open code"),
    ;
}
