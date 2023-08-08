package cc.unitmesh.devti.custom.variable

interface VariableResolver {
    val type: CustomIntentionVariableType
    fun resolve(): String

    fun variableName() = type.name
}