package cc.unitmesh.devti.settings

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo", "gpt-3.5-turbo-16k", "gpt-4")
val AI_ENGINES = arrayOf("OpenAI", "Custom", "Azure", "XingHuo")

enum class AIEngines {
    OpenAI, Custom, Azure, XingHuo
}

enum class XingHuoApiVersion(val value: Int) {
    V1(1), V2(2), V3(3);

    companion object {
        fun of(str: String): XingHuoApiVersion  = when (str) {
            "V1" -> V1
            "V2" -> V2
            "V3" -> V3
            else -> V3
        }
    }
}


val DEFAULT_AI_ENGINE = AI_ENGINES[0]

val HUMAN_LANGUAGES = arrayOf("English", "中文")
val DEFAULT_HUMAN_LANGUAGE = HUMAN_LANGUAGES[0]
val MAX_TOKEN_LENGTH = 4000
