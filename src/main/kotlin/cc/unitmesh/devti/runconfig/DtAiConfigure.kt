package cc.unitmesh.devti.runconfig

class DtAiConfigure(
    val githubToken: String,
    val openAiApiKey: String,
    val openAiEngine: String,
    val openAiMaxTokens: Int,
    val openAiTemperature: Float,
) {

    companion object {
        val DEFAULT_GITHUB_TOKEN = ""
        val DEFAULT_OPEN_AI_API_KEY = ""
        val DEFAULT_OPEN_AI_ENGINE = "gpt-3.5-turbo"
        val DEFAULT_OPEN_AI_MAX_TOKENS = 4096
        val DEFAULT_OPEN_AI_TEMPERATURE = 0.5f

        fun getDefault(): DtAiConfigure {
            return DtAiConfigure(
                DEFAULT_GITHUB_TOKEN,
                DEFAULT_OPEN_AI_API_KEY,
                DEFAULT_OPEN_AI_ENGINE,
                DEFAULT_OPEN_AI_MAX_TOKENS,
                DEFAULT_OPEN_AI_TEMPERATURE
            )
        }
    }
}