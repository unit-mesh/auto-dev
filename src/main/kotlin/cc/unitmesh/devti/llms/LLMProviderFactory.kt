package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.azure.AzureOpenAIProvider
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.llms.openai.OpenAIProvider
import cc.unitmesh.devti.llms.xianghuo.XingHuoProvider
import cc.unitmesh.devti.settings.AutoDevSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class LLMProviderFactory {
    private val aiEngine: String
        get() = AutoDevSettingsState.getInstance().aiEngine
    fun connector(project: Project): LLMProvider {
        return when (aiEngine) {
            // TODO use mapping and avoid hard code engine name
            "OpenAI" -> project.getService(OpenAIProvider::class.java)
            "Custom" -> project.getService(CustomLLMProvider::class.java)
            "Azure" -> project.getService(AzureOpenAIProvider::class.java)
            "XingHuo" -> project.getService(XingHuoProvider::class.java)
            else -> project.getService(OpenAIProvider::class.java)
        }
    }
}
