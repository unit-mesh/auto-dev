package cc.unitmesh.devti.custom.variable

class SelectionVariableResolver(
    private val languageName: String,
    val code: String,
) : VariableResolver {
    override val type: CustomIntentionVariableType = CustomIntentionVariableType.SELECTION

    override fun resolve(): String {
        return """
            |```$languageName
            |$code
            |```""".trimMargin()
    }
}