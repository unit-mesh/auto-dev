package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.azure.AzureOpenAIProvider
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.llms.openai.OpenAIProvider
import cc.unitmesh.devti.settings.DEFAULT_AI_ENGINE
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class LLMProviderFactory {
    private val aiEngine: String = AutoDevSettingsState.getInstance().aiEngine ?: DEFAULT_AI_ENGINE
    fun connector(project: Project): CodeCopilotProvider {
        return when (aiEngine) {
            "OpenAI" -> project.getService(OpenAIProvider::class.java)
            "Custom" -> project.getService(CustomLLMProvider::class.java)
            "Azure" -> project.getService(AzureOpenAIProvider::class.java)
            else -> project.getService(OpenAIProvider::class.java)
        }
    }

}