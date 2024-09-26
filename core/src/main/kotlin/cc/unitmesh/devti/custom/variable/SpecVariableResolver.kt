package cc.unitmesh.devti.custom.variable

class SpecVariableResolver(val key: String, val value: String) : VariableResolver {
    override val type: CustomResolvedVariableType get() = CustomResolvedVariableType.SPEC_VARIABLE

    override fun resolve(): String = value

    override fun variableName(): String = key
}
