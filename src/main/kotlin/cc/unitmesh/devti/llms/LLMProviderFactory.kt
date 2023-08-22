package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.azure.AzureOpenAIProvider
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.llms.openai.OpenAIProvider
import cc.unitmesh.devti.llms.xianghuo.XingHuoProvider
import cc.unitmesh.devti.settings.AIEngines
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class LLMProviderFactory {
    private val aiEngine: AIEngines
        get() = AIEngines.values().find { it.name.lowercase() == AutoDevSettingsState.getInstance().aiEngine.lowercase() } ?: AIEngines.OpenAI
    fun connector(project: Project): LLMProvider {
        return when (aiEngine) {
            AIEngines.OpenAI -> project.getService(OpenAIProvider::class.java)
            AIEngines.Custom -> project.getService(CustomLLMProvider::class.java)
            AIEngines.Azure -> project.getService(AzureOpenAIProvider::class.java)
            AIEngines.XingHuo -> project.getService(XingHuoProvider::class.java)
            else -> project.getService(OpenAIProvider::class.java)
        }
    }
}
