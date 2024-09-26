package cc.unitmesh.devti.custom.variable

class SelectionVariableResolver(
    private val languageName: String,
    private val code: String,
) : VariableResolver {
    override val type: CustomResolvedVariableType = CustomResolvedVariableType.SELECTION

    override fun resolve(): String {
        return """
            |```$languageName
            |$code
            |```""".trimMargin()
    }
}