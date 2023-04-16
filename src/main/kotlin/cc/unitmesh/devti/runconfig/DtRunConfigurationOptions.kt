package cc.unitmesh.devti.runconfig

import cc.unitmesh.devti.ai.OpenAIVersion
import com.intellij.execution.configurations.ModuleBasedConfigurationOptions
import com.intellij.openapi.components.StoredProperty
import com.intellij.openapi.diagnostic.Logger

class DtRunConfigurationOptions : ModuleBasedConfigurationOptions() {
    private val githubToken: StoredProperty<String?> = string("").provideDelegate(this, "githubToken")
    private val openAiApiKey: StoredProperty<String?> = string("").provideDelegate(this, "openAiApiKey")
    private val aiVersion: StoredProperty<Int> = property(1).provideDelegate(this, "aiVersion")
    private val maxTokens: StoredProperty<Int> = property(4096).provideDelegate(this, "aiMaxTokens")

    fun setFrom(configure: DevtiConfigure) {
        this.githubToken.setValue(this, configure.githubToken)
        this.openAiApiKey.setValue(this, configure.openAiApiKey)
        this.aiVersion.setValue(this, configure.aiVersion.index)
        this.maxTokens.setValue(this, configure.aiMaxTokens)
    }

    fun toConfigure(): DevtiConfigure {
        return DevtiConfigure(
            githubToken.getValue(this) ?: "",
            openAiApiKey.getValue(this) ?: "",
            OpenAIVersion.fromIndex(aiVersion.getValue(this) ?: 1),
            maxTokens.getValue(this) ?: 4096
        )
    }

    companion object {
        val logger = Logger.getInstance(DtRunConfigurationOptions::class.java)
    }
}
