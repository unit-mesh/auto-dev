package cc.unitmesh.devti.runconfig.options

import cc.unitmesh.devti.prompt.openai.DtOpenAIConfig
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.StoredProperty

open class OpenAIConfigureOptions : ModuleBasedConfigurationOptions() {
    val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    val aiEngineVersion: StoredProperty<Int> = property(0).provideDelegate(this, "aiEngineVersion")
    val maxTokens: StoredProperty<Int> =
        property(DtOpenAIConfig.DEFAULT_OPEN_AI_MAX_TOKENS).provideDelegate(this, "aiMaxTokens")

    fun openAiApiKey(): String = openAiApiKey.getValue(this) ?: ""
    fun setOpenAiApiKey(token: String) {
        openAiApiKey.setValue(this, token)
    }

    fun aiEngineVersion(): Int = aiEngineVersion.getValue(this)
    fun setAiEngineVersion(version: Int) {
        aiEngineVersion.setValue(this, version)
    }

    fun aiMaxTokens(): Int = maxTokens.getValue(this)
    fun setAiMaxTokens(tokens: Int) {
        maxTokens.setValue(this, tokens)
    }
}
