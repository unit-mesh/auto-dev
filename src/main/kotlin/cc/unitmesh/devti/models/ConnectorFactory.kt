package cc.unitmesh.devti.models

import cc.unitmesh.devti.models.azure.AzureOpenAIProvider
import cc.unitmesh.devti.models.custom.CustomProvider
import cc.unitmesh.devti.models.openai.OpenAIProvider
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class ConnectorFactory {
    private val aiEngine: String = AutoDevSettingsState.getInstance().aiEngine ?: DEFAULT_AI_ENGINE
    fun connector(project: Project): CodeCopilotProvider {
        return when (aiEngine) {
            "OpenAI" -> project.getService(OpenAIProvider::class.java)
            "Custom" -> project.getService(CustomProvider::class.java)
            "Azure" -> project.getService(AzureOpenAIProvider::class.java)
            else -> project.getService(OpenAIProvider::class.java)
        }
    }

    companion object {
        fun getInstance(): ConnectorFactory {
            return ApplicationManager.getApplication().getService(ConnectorFactory::class.java)
        }
    }
}