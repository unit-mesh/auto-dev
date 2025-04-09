package cc.unitmesh.devti.indexer.usage

import cc.unitmesh.devti.indexer.DomainDictService
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PromptEnhancer(val project: Project) {
    val templateRender: TemplateRender get() = TemplateRender(GENIUS_CODE)
    val template = templateRender.getTemplate("enhance.vm")

    fun create(input: String): String {
        val dict = project.getService(DomainDictService::class.java).loadContent() ?: ""
        val context = PromptEnhancerContext(dict, input)
        return templateRender.renderTemplate(template, context)
    }
}

data class PromptEnhancerContext(
    val dict: String,
    val userInput: String
) : TemplateContext {

}