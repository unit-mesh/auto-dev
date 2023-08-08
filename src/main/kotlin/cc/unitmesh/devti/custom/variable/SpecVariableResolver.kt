package cc.unitmesh.devti.custom.variable

class SpecVariableResolver(val key: String, val value: String) : VariableResolver {
    override val type: CustomIntentionVariableType get() = CustomIntentionVariableType.SPEC_VARIABLE

    override fun resolve(): String {
        return value
    }

    override fun variableName(): String {
        return key
    }
}