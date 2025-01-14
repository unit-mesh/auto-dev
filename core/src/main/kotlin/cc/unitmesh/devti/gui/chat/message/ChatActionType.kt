package cc.unitmesh.devti.gui.chat.message

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.prompting.TextTemplatePrompt
import cc.unitmesh.devti.template.GENIUS_CODE
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import cc.unitmesh.devti.template.context.TemplateContext
import com.intellij.openapi.project.Project

open class ChatTemplateContext(
    var language: String = "",
    var code: String = "",
) : TemplateContext

class GenApiTestContext(
    var baseUri: String = "",
    var frameworkContext: String = "",
    var requestBody: String = "",
    var relatedClasses: List<String> = emptyList(),
) : ChatTemplateContext()

enum class ChatActionType(var context: ChatTemplateContext) {
    CHAT(ChatTemplateContext()),
    REFACTOR(ChatTemplateContext()),
    EXPLAIN(ChatTemplateContext()),
    CODE_COMPLETE(ChatTemplateContext()),
    GENERATE_TEST(ChatTemplateContext()),
    GENERATE_TEST_DATA(GenApiTestContext()),
    GEN_COMMIT_MESSAGE(ChatTemplateContext()),
    FIX_ISSUE(ChatTemplateContext()),
    CREATE_CHANGELOG(ChatTemplateContext()),
    CREATE_GENIUS(ChatTemplateContext()),
    CUSTOM_COMPLETE(ChatTemplateContext()),
    CUSTOM_ACTION(ChatTemplateContext()),
    CUSTOM_AGENT(ChatTemplateContext()),
    CODE_REVIEW(ChatTemplateContext()),
    INLINE_CHAT(ChatTemplateContext()),
    SKETCH(ChatTemplateContext())
    ;

    fun instruction(lang: String = "", project: Project?): TextTemplatePrompt {
        return when (this) {
            EXPLAIN -> {
                val message = AutoDevBundle.message("prompts.autodev.explainCode", lang)
                TextTemplatePrompt(message, message)
            }

            REFACTOR -> {
                val displayText = AutoDevBundle.message("prompts.autodev.refactorCode", lang)
                val templateRender = TemplateRender(GENIUS_PRACTISES)
                val template = templateRender.getTemplate("refactoring.vm")

                TextTemplatePrompt(displayText, template, templateRender, this.context)
            }

            CODE_COMPLETE -> {
                val text = AutoDevBundle.message("prompts.autodev.completeCode", lang)
                TextTemplatePrompt(text, text)
            }

            GENERATE_TEST -> {
                val text = AutoDevBundle.message("prompts.autodev.generateTest", lang)
                TextTemplatePrompt(text, text)
            }

            FIX_ISSUE -> {
                val text = AutoDevBundle.message("prompts.autodev.fixProblem", lang)
                TextTemplatePrompt(text, text)
            }

            GEN_COMMIT_MESSAGE -> TextTemplatePrompt("", "")
            CREATE_CHANGELOG -> {
                val text = AutoDevBundle.message("prompts.autodev.generateReleaseNote")
                TextTemplatePrompt(text, text)
            }

            CHAT -> TextTemplatePrompt("", "")
            CUSTOM_COMPLETE -> TextTemplatePrompt("", "")
            CUSTOM_ACTION -> TextTemplatePrompt("", "")
            CUSTOM_AGENT -> TextTemplatePrompt("", "")
            CODE_REVIEW -> TextTemplatePrompt("", "")
            CREATE_GENIUS -> TextTemplatePrompt("", "")
            GENERATE_TEST_DATA -> {
                val displayText = AutoDevBundle.message("prompts.autodev.generateTestData", lang)
                val templateRender = TemplateRender(GENIUS_CODE)
                val template = templateRender.getTemplate("api-test-gen.vm")

                TextTemplatePrompt(displayText, template, templateRender, this.context)
            }

            INLINE_CHAT -> {
                val displayText = AutoDevBundle.message("prompts.autodev.inlineChat", lang)
                val templateRender = TemplateRender(GENIUS_CODE)
                val template = templateRender.getTemplate("inline-chat.devin")

                TextTemplatePrompt(displayText, template, templateRender, this.context)
            }

            SKETCH -> {
                val displayText = AutoDevBundle.message("prompts.autodev.sketch", lang)
                val templateRender = TemplateRender(GENIUS_CODE)
                val template = templateRender.getTemplate("sketch.vm")

                TextTemplatePrompt(displayText, template, templateRender, this.context)
            }
        }
    }
}
