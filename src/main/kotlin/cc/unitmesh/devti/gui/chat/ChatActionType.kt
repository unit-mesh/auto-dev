package cc.unitmesh.devti.gui.chat

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
    COUNIT,
    CODE_REVIEW
    ;

    fun instruction(lang: String = "", project: Project?): String {
        val devCoderSettings = project?.coderSetting?.state

        return when (this) {
            EXPLAIN -> {
                devCoderSettings?.explainCode.let {
                    val defaultPrompt = "Explain $lang code"
                    compilePrompt(it, defaultPrompt, lang)
                }
            }
            REFACTOR -> {
                devCoderSettings?.refactorCode.let {
                    val defaultPrompt = "Refactor the given $lang code"
                    compilePrompt(it, defaultPrompt, lang)
                }
            }
            CODE_COMPLETE -> "Complete $lang code, return rest code, no explaining"
            GENERATE_TEST -> {
                devCoderSettings?.generateTest.let {
                    val defaultPrompt = "Generate test for $lang code"
                    compilePrompt(it, defaultPrompt, lang)
                }
            }
            FIX_ISSUE -> {
                devCoderSettings?.fixIssueCode.let {
                    val defaultPrompt = "Help me fix this issue"
                    compilePrompt(it, defaultPrompt, lang)
                }
            }
            GEN_COMMIT_MESSAGE -> ""
            CREATE_CHANGELOG -> "generate release note"
            CHAT -> ""
            CUSTOM_COMPLETE -> ""
            CUSTOM_ACTION -> ""
            COUNIT -> ""
            CODE_REVIEW -> ""
            CREATE_GENIUS -> ""
            GENERATE_TEST_DATA -> "Generate JSON for given $lang code. So that we can use it to test for APIs. \n" +
                    "Make sure JSON contains real business logic, not just data structure. \n" +
                    "For example, if the code is a function that returns a list of users, " +
                    "the JSON should contain a list of users, not just a list of user objects."
        }
    }

    private fun compilePrompt(it: String?, defaultPrompt: String, lang: String) = if (it.isNullOrEmpty()) {
        defaultPrompt
    } else {
        it.replace("\$lang", lang)
    }
}
