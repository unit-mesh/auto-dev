package cc.unitmesh.devti.connector

import cc.unitmesh.devti.connector.custom.CustomConnector
import cc.unitmesh.devti.connector.openai.OpenAIConnector
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.DevtiSettingsState

class ConnectorDispatcher {
    private val aiEngine: String = DevtiSettingsState.getInstance()?.aiEngine ?: DEFAULT_AI_ENGINE
    fun getConnector(): CodeCopilot {
        return when (aiEngine) {
            "OpenAI" -> OpenAIConnector()
            "Custom" -> CustomConnector()
            else -> OpenAIConnector()
        }
    }
}