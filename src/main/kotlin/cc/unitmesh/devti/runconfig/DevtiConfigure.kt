package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.ai.OpenAIVersions
import cc.unitmesh.devti.runconfig.command.BaseConfig

class DevtiConfigure(
    var githubToken: String,
    var openAiApiKey: String,
    var aiVersions: OpenAIVersions,
    var openAiMaxTokens: Int,
    var openAiTemperature: Float,
) : BaseConfig() {
    // match configure name for story name
    override val configurationName = "DevTi"

    companion object {
        val DEFAULT_GITHUB_TOKEN = ""
        val DEFAULT_OPEN_AI_API_KEY = ""
        val DEFAULT_OPEN_AI_MAX_TOKENS = 4096
        val DEFAULT_OPEN_AI_TEMPERATURE = 0.5f

        fun getDefault(): DevtiConfigure {
            return DevtiConfigure(
                DEFAULT_GITHUB_TOKEN,
                DEFAULT_OPEN_AI_API_KEY,
                OpenAIVersions.DEFAULT,
                DEFAULT_OPEN_AI_MAX_TOKENS,
                DEFAULT_OPEN_AI_TEMPERATURE
            )
        }
    }
}