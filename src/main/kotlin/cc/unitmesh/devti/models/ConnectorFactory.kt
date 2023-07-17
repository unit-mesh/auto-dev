package cc.unitmesh.devti.models

import cc.unitmesh.devti.models.azure.AzureOpenAIProvider
import cc.unitmesh.devti.models.custom.CustomProvider
import cc.unitmesh.devti.models.openai.OpenAIProvider
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.application.ApplicationManager

class ConnectorFactory {
    private val aiEngine: String = AutoDevSettingsState.getInstance().aiEngine ?: DEFAULT_AI_ENGINE
    fun connector(): CodeCopilotProvider {
        return when (aiEngine) {
            "OpenAI" -> OpenAIProvider()
            "Custom" -> CustomProvider()
            "Azure" -> AzureOpenAIProvider()
            else -> OpenAIProvider()
        }
    }

    companion object {
        fun getInstance(): ConnectorFactory {
            return ApplicationManager.getApplication().getService(ConnectorFactory::class.java)
        }
    }
}