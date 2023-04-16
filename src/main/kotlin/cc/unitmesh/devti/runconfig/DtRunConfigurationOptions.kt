package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.prompt.openai.OpenAI.DEFAULT_OPEN_AI_MAX_TOKENS
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class DtRunConfigurationOptions : ModuleBasedConfigurationOptions() {
    var githubToken: StoredProperty<String?> = string("").provideDelegate(this, "githubToken")

    private val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    private val aiVersion: StoredProperty<Int> = property(0).provideDelegate(this, "aiVersion")
    private val maxTokens: StoredProperty<Int> =
        property(DEFAULT_OPEN_AI_MAX_TOKENS).provideDelegate(this, "aiMaxTokens")

    fun githubToken(): String = githubToken.getValue(this) ?: ""
    fun setGithubToken(token: String) {
        githubToken.setValue(this, token)
    }

    fun openAiApiKey(): String = openAiApiKey.getValue(this) ?: ""
    fun setOpenAiApiKey(token: String) {
        openAiApiKey.setValue(this, token)
    }

    fun aiVersion(): Int = aiVersion.getValue(this)
    fun setAiVersion(version: Int) {
        aiVersion.setValue(this, version)
    }

    fun aiMaxTokens(): Int = maxTokens.getValue(this)
    fun setAiMaxTokens(tokens: Int) {
        maxTokens.setValue(this, tokens)
    }
}
