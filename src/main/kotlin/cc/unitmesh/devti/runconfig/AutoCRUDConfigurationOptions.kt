package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.prompt.openai.DtOpenAIConfig.DEFAULT_OPEN_AI_MAX_TOKENS
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class AutoCRUDConfigurationOptions : ModuleBasedConfigurationOptions() {
    private var githubToken: StoredProperty<String?> = string("").provideDelegate(this, "githubToken")

    private val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    private val aiEngineVersion: StoredProperty<Int> = property(0).provideDelegate(this, "aiEngineVersion")
    private val maxTokens: StoredProperty<Int> =
        property(DEFAULT_OPEN_AI_MAX_TOKENS).provideDelegate(this, "aiMaxTokens")
    private val githubRepo: StoredProperty<String?> = string("unit-mesh/untitled").provideDelegate(this, "githubRepo")
    private val storyId: StoredProperty<String?> = string("1").provideDelegate(this, "storyId")

    fun githubToken(): String = githubToken.getValue(this) ?: ""
    fun setGithubToken(token: String) {
        githubToken.setValue(this, token)
    }

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
    fun githubRepo(): String = githubRepo.getValue(this) ?: ""
    fun setGithubRepo(repo: String) {
        githubRepo.setValue(this, repo)
    }

    fun storyId(): String = storyId.getValue(this) ?: ""
    fun setStoryId(id: String) {
        storyId.setValue(this, id)
    }
}
