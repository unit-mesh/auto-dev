package cc.unitmesh.devti.language.compiler

data class CompileResult(
    var output: String = "",
    var isLocalCommand: Boolean = false,
    var hasError: Boolean = false
)