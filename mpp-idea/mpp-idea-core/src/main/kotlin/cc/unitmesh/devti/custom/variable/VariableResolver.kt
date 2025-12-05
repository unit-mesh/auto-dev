package cc.unitmesh.devti.custom.variable

interface VariableResolver {
    val type: CustomResolvedVariableType
    fun resolve(): String
    fun variableName() = type.name
}