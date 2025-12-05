package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llm2.model.LlmConfig
import cc.unitmesh.devti.llm2.model.ModelType
import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import com.intellij.openapi.project.Project

object LlmFactory {
    fun create(project: Project): LLMProvider {
        return LLMProviderAdapter(project)
    }

    /**
     * New LLMProvider with custom config, for easy to migration old llm provider
     */
    fun create(project: Project, modelType: ModelType): LLMProvider {
        return LLMProviderAdapter(project, modelType)
    }

    fun createCompletion(project: Project): LLMProvider {
        return LLMProviderAdapter(project, ModelType.Completion)
    }

    @Deprecated("Use create() instead", ReplaceWith("create(project)"))
    fun createLegacy(project: Project): LLMProvider {
        return CustomLLMProvider(project)
    }

    @Deprecated("Use create(project, modelType) instead", ReplaceWith("create(project, modelType)"))
    fun createLegacy(project: Project, modelType: ModelType): LLMProvider {
        val llmConfigs = LlmConfig.load(modelType)
        val llmConfig = llmConfigs.firstOrNull() ?: LlmConfig.default()
        return CustomLLMProvider(project, llmConfig)
    }
}
