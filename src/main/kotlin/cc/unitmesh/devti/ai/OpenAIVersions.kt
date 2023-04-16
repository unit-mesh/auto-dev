package cc.unitmesh.devti.ai

enum class OpenAIVersions(val index: Int, val title: String) {
    GPT35TURBO(0, "gpt-3.5-turbo"),
    GPT40(1, "gpt-4.0");

    override fun toString(): String = title

    companion object {
        @JvmField
        val DEFAULT: OpenAIVersions = GPT35TURBO

        fun fromIndex(index: Int): OpenAIVersions = values().find { it.index == index } ?: DEFAULT
    }

}