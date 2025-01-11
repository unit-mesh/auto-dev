package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.llms.custom.InlayCustomLLMProvider
import cc.unitmesh.devti.settings.coder.AutoDevCoderSettingService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

@Service
class LlmFactory {
    fun create(project: Project): LLMProvider {
        return project.getService(CustomLLMProvider::class.java)
    }

    fun createForInlayCodeComplete(project: Project): LLMProvider {
        if(project.service<AutoDevCoderSettingService>().state.useCustomAIEngineWhenInlayCodeComplete) {
            logger<LlmFactory>().info("useCustomAIEngineWhenInlayCodeComplete: ${project.service<AutoDevCoderSettingService>().state.useCustomAIEngineWhenInlayCodeComplete}")
            return project.getService(InlayCustomLLMProvider::class.java)
        }

        return create(project);
    }

    companion object {
        val instance: LlmFactory = LlmFactory()
    }
}
