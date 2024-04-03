package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.prompting.BasicTextPrompt
import cc.unitmesh.devti.template.GENIUS_PRACTISES
import cc.unitmesh.devti.template.TemplateRender
import com.intellij.openapi.project.Project

enum class ChatActionType {
    CHAT,
    REFACTOR,
    EXPLAIN,
    CODE_COMPLETE,
    GENERATE_TEST,
    GENERATE_TEST_DATA,
    GEN_COMMIT_MESSAGE,
    FIX_ISSUE,
    CREATE_CHANGELOG,
    CREATE_GENIUS,
    CUSTOM_COMPLETE,
    CUSTOM_ACTION,
    CUSTOM_AGENT,
    CODE_REVIEW
    ;

    fun instruction(lang: String = "", project: Project?): BasicTextPrompt {
        return when (this) {
            EXPLAIN -> {
                val message = AutoDevBundle.message("prompts.autodev.explainCode", lang)
                BasicTextPrompt(message, message)
            }

            REFACTOR -> {
                val displayText = AutoDevBundle.message("prompts.autodev.refactorCode", lang)
                val templateRender = TemplateRender(GENIUS_PRACTISES)
                val template = templateRender.getTemplate("refactoring.vm")
                val requestPrompt = templateRender.renderTemplate(template)

                BasicTextPrompt(displayText, requestPrompt)
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
                val text = AutoDevBundle.message("prompts.autodev.generateTestData", lang)
                BasicTextPrompt(text, text)
            }
        }
    }
}
