package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.prompting.BasicTextPrompt
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.EmptyContext
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.project.Project

data class GenApiTestContext(
    var language: String = "",
    var baseUri: String = "",
    var frameworkContext: String = "",
    var requestBody: String = "",
    var relatedClasses: List<String> = emptyList(),
    var code: String = "",
) : TemplateContext {

}

enum class ChatActionType(var context: TemplateContext) {
    CHAT(EmptyContext()),
    REFACTOR(EmptyContext()),
    EXPLAIN(EmptyContext()),
    CODE_COMPLETE(EmptyContext()),
    GENERATE_TEST(EmptyContext()),
    GENERATE_TEST_DATA(GenApiTestContext()),
    GEN_COMMIT_MESSAGE(EmptyContext()),
    FIX_ISSUE(EmptyContext()),
    CREATE_CHANGELOG(EmptyContext()),
    CREATE_GENIUS(EmptyContext()),
    CUSTOM_COMPLETE(EmptyContext()),
    CUSTOM_ACTION(EmptyContext()),
    CUSTOM_AGENT(EmptyContext()),
    CODE_REVIEW(EmptyContext())
    ;

    fun instruction(lang: String = "", project: Project?, context: TemplateContext? = null): BasicTextPrompt {
        return when (this) {
            EXPLAIN -> {
                val message = AutoDevBundle.message("prompts.autodev.explainCode", lang)
                BasicTextPrompt(message, message)
            }

            REFACTOR -> {
                val displayText = AutoDevBundle.message("prompts.autodev.refactorCode", lang)
                val templateRender = TemplateRender(GENIUS_PRACTISES)
                val template = templateRender.getTemplate("refactoring.vm")

                BasicTextPrompt(displayText, template, templateRender, EmptyContext())
            }

            CODE_COMPLETE -> {
                val text = AutoDevBundle.message("prompts.autodev.completeCode", lang)
                BasicTextPrompt(text, text)
            }

            GENERATE_TEST -> {
                val text = AutoDevBundle.message("prompts.autodev.generateTest", lang)
                BasicTextPrompt(text, text)
            }

            FIX_ISSUE -> {
                val text = AutoDevBundle.message("prompts.autodev.fixProblem", lang)
                BasicTextPrompt(text, text)
            }

            GEN_COMMIT_MESSAGE -> BasicTextPrompt("", "")
            CREATE_CHANGELOG -> {
                val text = AutoDevBundle.message("prompts.autodev.generateReleaseNote")
                BasicTextPrompt(text, text)
            }

            CHAT -> BasicTextPrompt("", "")
            CUSTOM_COMPLETE -> BasicTextPrompt("", "")
            CUSTOM_ACTION -> BasicTextPrompt("", "")
            CUSTOM_AGENT -> BasicTextPrompt("", "")
            CODE_REVIEW -> BasicTextPrompt("", "")
            CREATE_GENIUS -> BasicTextPrompt("", "")
            GENERATE_TEST_DATA -> {
                val displayText = AutoDevBundle.message("prompts.autodev.generateTestData", lang)
                val templateRender = TemplateRender(GENIUS_CODE)
                val template = templateRender.getTemplate("api-test-gen.vm")

                BasicTextPrompt(displayText, template, templateRender, GenApiTestContext())
            }
        }
    }
}
