package cc.unitmesh.devti.prompt.openai

enum class DtOpenAIVersion(val index: Int, val title: String) {
    GPT35TURBO(0, "gpt-3.5-turbo"),
    GPT40(1, "gpt-4.0");

    override fun toString(): String = title

    companion object {
        @JvmField
        val DEFAULT: DtOpenAIVersion = GPT35TURBO

        fun fromIndex(index: Int): DtOpenAIVersion = values().find { it.index == index } ?: DEFAULT
    }
}
