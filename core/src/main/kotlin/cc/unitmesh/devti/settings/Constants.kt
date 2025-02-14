package cc.unitmesh.devti.settings

val AI_ENGINES = arrayOf("OpenAI", "Custom")

enum class ResponseType {
    SSE, JSON;
}

val DEFAULT_AI_ENGINE = AI_ENGINES[0]

@Suppress("unused")
enum class HUMAN_LANGUAGES(val abbr: String, val display: String) {
    ENGLISH("en", "English"),
    CHINESE("zh", "中文");

    companion object {
        private val map: Map<String, HUMAN_LANGUAGES> = HUMAN_LANGUAGES.values().map { it.display to it }.toMap()

        fun getAbbrByDispay(display: String): String {
            return map.getOrDefault(display, ENGLISH).abbr
        }
    }
}
val DEFAULT_HUMAN_LANGUAGE = HUMAN_LANGUAGES.ENGLISH.display
val MAX_TOKEN_LENGTH = 4000
val SELECT_CUSTOM_MODEL = "custom"
