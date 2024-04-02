package cc.unitmesh.devti.gui.chat

import cc.unitmesh.devti.AutoDevBundle
import cc.unitmesh.devti.settings.coder.coderSetting
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

    fun instruction(lang: String = "", project: Project?): String {
        val devCoderSettings = project?.coderSetting?.state

        return when (this) {
            EXPLAIN -> AutoDevBundle.message("prompts.autodev.explainCode", lang)
            REFACTOR -> AutoDevBundle.message("prompts.autodev.refactorCode", lang)

            CODE_COMPLETE -> AutoDevBundle.message("prompts.autodev.completeCode", lang)
            GENERATE_TEST -> AutoDevBundle.message("prompts.autodev.generateTest", lang)

            FIX_ISSUE -> AutoDevBundle.message("prompts.autodev.fixProblem", lang)

            GEN_COMMIT_MESSAGE -> ""
            CREATE_CHANGELOG -> AutoDevBundle.message("prompts.autodev.generateReleaseNote")
            CHAT -> ""
            CUSTOM_COMPLETE -> ""
            CUSTOM_ACTION -> ""
            CUSTOM_AGENT -> ""
            CODE_REVIEW -> ""
            CREATE_GENIUS -> ""
            GENERATE_TEST_DATA -> AutoDevBundle.message("prompts.autodev.generateTestData", lang)
        }
    }

}
