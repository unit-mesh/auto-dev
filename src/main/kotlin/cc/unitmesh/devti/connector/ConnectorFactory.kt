package cc.unitmesh.devti.connector

import cc.unitmesh.devti.connector.azure.AzureConnector
import cc.unitmesh.devti.connector.custom.CustomConnector
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.application.ApplicationManager

class ConnectorFactory {
    private val aiEngine: String = AutoDevSettingsState.getInstance()?.aiEngine ?: DEFAULT_AI_ENGINE
    fun connector(): CodeCopilot {
        return when (aiEngine) {
            "OpenAI" -> OpenAIConnector()
            "Custom" -> CustomConnector()
            "Azure" -> AzureConnector()
            else -> OpenAIConnector()
        }
    }

    companion object {
        fun getInstance(): ConnectorFactory {
            return ApplicationManager.getApplication().getService(ConnectorFactory::class.java)
        }
    }
}