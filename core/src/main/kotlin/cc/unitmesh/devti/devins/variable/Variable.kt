package cc.unitmesh.devti.devins.variable

interface Variable {
    val variableName: String
    val description: String
    var value: Any?
}
