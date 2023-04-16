package cc.unitmesh.devti.runconfig.config

import cc.unitmesh.devti.ai.OpenAIVersion
import cc.unitmesh.devti.runconfig.command.BaseConfig

class DevtiCreateStoryConfigure(
    var githubToken: String,
    var openAiApiKey: String,
    var aiVersion: OpenAIVersion,
    var aiMaxTokens: Int,
) : BaseConfig() {
    override val configurationName = "DevTi Configure"

    companion object {
        const val DEFAULT_GITHUB_TOKEN = ""
        const val DEFAULT_OPEN_AI_API_KEY = ""
        const val DEFAULT_OPEN_AI_MAX_TOKENS = 4096

        fun getDefault(): DevtiCreateStoryConfigure {
            return DevtiCreateStoryConfigure(
                DEFAULT_GITHUB_TOKEN,
                DEFAULT_OPEN_AI_API_KEY,
                OpenAIVersion.DEFAULT,
                DEFAULT_OPEN_AI_MAX_TOKENS
            )
        }
    }
}