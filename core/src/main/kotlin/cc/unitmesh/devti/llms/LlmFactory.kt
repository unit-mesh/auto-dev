package cc.unitmesh.devti.llms

import cc.unitmesh.devti.llms.custom.CustomLLMProvider
import cc.unitmesh.devti.llms.custom.InlayCustomLLMProvider
import cc.unitmesh.devti.settings.coder.AutoDevCoderSettingService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project

object LlmFactory {
    fun create(project: Project): LLMProvider {
        return CustomLLMProvider(project)
    }

    fun createForInlayCodeComplete(project: Project): LLMProvider {
        if(project.service<AutoDevCoderSettingService>().state.useCustomAIEngineWhenInlayCodeComplete) {
            logger<LlmFactory>().info("useCustomAIEngineWhenInlayCodeComplete: ${project.service<AutoDevCoderSettingService>().state.useCustomAIEngineWhenInlayCodeComplete}")
            return InlayCustomLLMProvider(project)
        }

        return create(project);
    }

}
