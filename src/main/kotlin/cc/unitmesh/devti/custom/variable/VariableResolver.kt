package cc.unitmesh.devti.custom.variable

interface VariableResolver {
    val type: CustomVariableType
    fun resolve(): String
    fun variableName() = type.name
}