package cc.unitmesh.devti.settings

val OPENAI_MODEL = arrayOf("gpt-3.5-turbo","gpt-3.5-turbo-16k", "gpt-4")
val AI_ENGINES = arrayOf("OpenAI", "Custom", "Azure", "XingHuo")

enum class AIEngines {
    OpenAI, Custom, Azure, XingHuo
}

val GIT_TYPE = arrayOf("Github" , "Gitlab")
val DEFAULT_GIT_TYPE = GIT_TYPE[0]
val DEFAULT_AI_ENGINE = AI_ENGINES[0]

val HUMAN_LANGUAGES = arrayOf("English", "中文")
val DEFAULT_HUMAN_LANGUAGE = HUMAN_LANGUAGES[0]
val MAX_TOKEN_LENGTH = 4000
