package cc.unitmesh.devti.language.ast.variable

interface Variable {
    val variableName: String
    val description: String
    var value: Any?
}
